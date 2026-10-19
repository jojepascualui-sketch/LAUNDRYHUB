package com.laundryhub.app.data.rider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.api.ApiClient;
import com.laundryhub.app.data.api.TokenProvider;
import com.laundryhub.app.data.api.dto.RiderJobDto;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.PaymentOption;
import com.laundryhub.app.model.rider.JobType;
import com.laundryhub.app.model.rider.RiderJob;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/** JSON from /api/rider/jobs → RiderJob, the cash step endpoint and the 409 on accept. */
public class RemoteRiderRepositoryTest {

    private static final String DELIVERY_JSON = "{\"id\":8,\"orderNo\":\"LH-0008\",\"type\":\"RETURN_DELIVERY\","
            + "\"status\":\"IN_PROGRESS\",\"distanceKm\":2.1,\"customerName\":\"Carlo Mendoza\","
            + "\"customerInitials\":\"CM\",\"customerMobile\":\"09171234567\","
            + "\"customerPin\":{\"latitude\":14.612,\"longitude\":120.995,\"addressDetails\":\"Brgy. Mabini\","
            + "\"geocodedAddress\":\"12 Rizal St, Manila\"},\"shop\":{\"latitude\":14.5995,\"longitude\":120.9842},"
            + "\"scheduleText\":\"Today, 4–6 PM\",\"billableKg\":9,\"total\":330.00,"
            + "\"payment\":{\"method\":\"CASH\",\"paid\":false,\"collectCash\":true,\"amountDue\":330.00},"
            + "\"steps\":[{\"title\":\"Collect laundry at shop\",\"actionLabel\":\"Picked up at shop\"},"
            + "{\"title\":\"On the way to customer\",\"actionLabel\":\"Start trip\"},"
            + "{\"title\":\"Arrived at drop-off\",\"actionLabel\":\"I’ve arrived\"},"
            + "{\"title\":\"Cash collected\",\"actionLabel\":\"Cash received\"},"
            + "{\"title\":\"Delivered\",\"actionLabel\":\"Mark as delivered\"}],"
            + "\"currentStepIndex\":3,\"awaitingCash\":true}";

    private MockWebServer server;
    private RemoteRiderRepository repository;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        repository = new RemoteRiderRepository(new ApiClient(server.url("/api/").toString(), new TokenProvider() {
            @Override
            public String getToken() {
                return "token";
            }

            @Override
            public void onUnauthorized() {
            }
        }).rider());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void mapsTheServerJob() {
        RiderJob job = RemoteRiderRepository.toJob(new Gson().fromJson(DELIVERY_JSON, RiderJobDto.class));

        assertEquals("LH-0008", job.getOrderNo());
        assertEquals(JobType.RETURN_DELIVERY, job.getType());
        assertEquals(PaymentOption.CASH, job.getPaymentMethod());
        assertEquals("Brgy. Mabini · 12 Rizal St, Manila", job.getAddress());
        assertEquals(Location.of(14.612, 120.995), job.nextStop());
        assertEquals("Cash received", job.currentActionLabel());
        assertTrue(job.isAwaitingCash());
        assertEquals(new BigDecimal("2.1"), job.getDistanceKm());
    }

    @Test
    public void unknownStatusIsOther() {
        RiderJob job = RemoteRiderRepository.toJob(new Gson().fromJson(
                DELIVERY_JSON.replace("IN_PROGRESS", "CANCELLED"), RiderJobDto.class));
        assertEquals(RiderJob.Status.OTHER, job.getStatus());
    }

    @Test
    public void cashStepCallsCashCollected() throws Exception {
        server.enqueue(new MockResponse().setBody(DELIVERY_JSON.replace("\"currentStepIndex\":3,\"awaitingCash\":true",
                "\"currentStepIndex\":4,\"awaitingCash\":false")));
        RiderJob job = RemoteRiderRepository.toJob(new Gson().fromJson(DELIVERY_JSON, RiderJobDto.class));

        Result<RiderJob> result = new Result<>();
        repository.completeStep(job, result);
        result.await();

        RecordedRequest request = server.takeRequest();
        assertEquals("/api/rider/jobs/8/cash-collected", request.getPath());
        assertEquals("Mark as delivered", result.value.get().currentActionLabel());
    }

    @Test
    public void otherStepsCallAdvance() throws Exception {
        String atZero = DELIVERY_JSON.replace("\"currentStepIndex\":3,\"awaitingCash\":true",
                "\"currentStepIndex\":0,\"awaitingCash\":false");
        server.enqueue(new MockResponse().setBody(atZero));
        Result<RiderJob> result = new Result<>();
        repository.completeStep(RemoteRiderRepository.toJob(new Gson().fromJson(atZero, RiderJobDto.class)), result);
        result.await();

        assertEquals("/api/rider/jobs/8/advance", server.takeRequest().getPath());
    }

    @Test
    public void takenBookingIs409WithTheServerMessage() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(409).setHeader("Content-Type", "application/problem+json")
                .setBody("{\"status\":409,\"detail\":\"This booking was already taken by another rider.\"}"));
        Result<RiderJob> result = new Result<>();
        repository.accept(8, result);
        result.await();

        assertNull(result.value.get());
        assertEquals(RiderRepository.ALREADY_TAKEN, result.error.get().getStatus());
        assertEquals("/api/rider/jobs/8/accept", server.takeRequest().getPath());
    }

    @Test
    public void declineAcceptsAnEmpty204() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));
        Result<Boolean> result = new Result<>();
        repository.decline(8, result);
        result.await();

        assertEquals(Boolean.TRUE, result.value.get());
    }

    @Test
    public void addressFallsBackToWhatExists() {
        RiderJobDto.Pin onlyGeocoded = new Gson().fromJson("{\"geocodedAddress\":\"12 Rizal St\"}", RiderJobDto.Pin.class);
        assertEquals("12 Rizal St", RemoteRiderRepository.address(onlyGeocoded));
        RiderJobDto.Pin same = new Gson().fromJson("{\"addressDetails\":\"A\",\"geocodedAddress\":\"A\"}",
                RiderJobDto.Pin.class);
        assertEquals("A", RemoteRiderRepository.address(same));
        assertEquals("", RemoteRiderRepository.address(null));
    }

    /** Waits for the OkHttp callback thread. */
    private static final class Result<T> implements ApiCallback<T> {
        final AtomicReference<T> value = new AtomicReference<>();
        final AtomicReference<ApiException> error = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);

        @Override
        public void onSuccess(@NonNull T v) {
            value.set(v);
            done.countDown();
        }

        @Override
        public void onError(@NonNull ApiException e) {
            error.set(e);
            done.countDown();
        }

        void await() throws InterruptedException {
            assertTrue("no callback", done.await(5, TimeUnit.SECONDS));
        }
    }
}
