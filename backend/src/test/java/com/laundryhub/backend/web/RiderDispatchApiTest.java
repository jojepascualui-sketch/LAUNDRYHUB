package com.laundryhub.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/** Rider app R1–R5: duty, bookings sorted by distance, accept/decline races and step workflow. */
class RiderDispatchApiTest extends ApiTestBase {

    private static final String RIDER = "/api/rider";

    private void at(String riderToken, double lat, double lng) throws Exception {
        postJson(RIDER + "/location", riderToken, Map.of("lat", lat, "lng", lng, "accuracyMeters", 8))
                .andExpect(status().isNoContent());
    }

    private void advance(String riderToken, long jobId) throws Exception {
        postJson(RIDER + "/jobs/" + jobId + "/advance", riderToken, null).andExpect(status().isOk());
    }

    @Test
    void availableBookingsAreSortedByDistanceAndHiddenWhenOffDuty() throws Exception {
        String customer = customer("ana@example.com");
        long far = placeOrder(customer, deliveryOrder(12, "Premium Scented", FARTHER_PIN, "CASH"));
        long near = placeOrder(customer, deliveryOrder(9, "Standard", NEAR_PIN, "CASH"));
        String onDuty = rider("Rider One", "rider1@example.com", true);
        String offDuty = rider("Rider Two", "rider2@example.com", false);
        at(onDuty, 14.605, 121.005);

        getJson(RIDER + "/jobs/available", onDuty)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].orderId").value(near))
                .andExpect(jsonPath("$[0].typeLabel").value("Pickup"))
                .andExpect(jsonPath("$[0].distanceKm").value(0.8))
                .andExpect(jsonPath("$[0].scheduleText").value("Today, 2–4 PM"))
                .andExpect(jsonPath("$[0].customerMobile").doesNotExist())
                .andExpect(jsonPath("$[0].payment.method").value("CASH"))
                .andExpect(jsonPath("$[1].orderId").value(far))
                .andExpect(jsonPath("$[1].billableKg").value(12.00))
                .andExpect(jsonPath("$[1].total").value(516.00));
        getJson(RIDER + "/jobs/available", offDuty).andExpect(jsonPath("$", hasSize(0)));

        patchJson(RIDER + "/duty", offDuty, Map.of("onDuty", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onDuty").value(true))
                .andExpect(jsonPath("$.availableCount").value(2));
        getJson(RIDER + "/jobs/available", offDuty).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void declineHidesTheJobForThatRiderOnly() throws Exception {
        long order = placeOrder(customer("ana@example.com"), deliveryOrder(10, "Standard", NEAR_PIN, "CASH"));
        long pickup = jobId(order, "PICKUP");
        String one = rider("Rider One", "rider1@example.com", true);
        String two = rider("Rider Two", "rider2@example.com", true);

        postJson(RIDER + "/jobs/" + pickup + "/decline", two, null).andExpect(status().isNoContent());

        getJson(RIDER + "/jobs/available", two).andExpect(jsonPath("$", hasSize(0)));
        getJson(RIDER + "/jobs/available", one).andExpect(jsonPath("$", hasSize(1)));
        assertThat(jdbc.queryForObject("SELECT status FROM rider_jobs WHERE id = ?", String.class, pickup))
                .isEqualTo("AVAILABLE");
    }

    @Test
    void secondRiderToAcceptGetsConflict() throws Exception {
        long order = placeOrder(customer("ana@example.com"), deliveryOrder(10, "Standard", NEAR_PIN, "CASH"));
        long pickup = jobId(order, "PICKUP");
        String one = rider("Rider One", "rider1@example.com", true);
        String two = rider("Rider Two", "rider2@example.com", true);

        postJson(RIDER + "/jobs/" + pickup + "/accept", one, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.customerMobile").value("09170000001"))
                .andExpect(jsonPath("$.currentActionLabel").value("Start trip"));
        postJson(RIDER + "/jobs/" + pickup + "/accept", two, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This booking was already taken by another rider."));
        getJson(RIDER + "/jobs/available", two).andExpect(jsonPath("$", hasSize(0)));
        getJson(RIDER + "/jobs/" + pickup, two).andExpect(status().isNotFound());
    }

    @Test
    void simultaneousAcceptsHaveExactlyOneWinner() throws Exception {
        long order = placeOrder(customer("ana@example.com"), deliveryOrder(10, "Standard", NEAR_PIN, "CASH"));
        long pickup = jobId(order, "PICKUP");
        List<String> riders = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            riders.add(rider("Rider " + i, "rider" + i + "@example.com", true));
        }

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(riders.size());
        List<Future<Integer>> results = new ArrayList<>();
        for (String token : riders) {
            Callable<Integer> tap = () -> {
                start.await();
                return postJson(RIDER + "/jobs/" + pickup + "/accept", token, null).andReturn().getResponse()
                        .getStatus();
            };
            results.add(pool.submit(tap));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(riders.size() - 1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rider_jobs WHERE id = ? AND rider_id IS NOT NULL",
                Integer.class, pickup)).isEqualTo(1);
    }

    @Test
    void cashDeliveryRunsThroughEveryStepAndMovesTheOrder() throws Exception {
        String customer = customer("ana@example.com");
        String admin = admin();
        long order = placeOrder(customer, deliveryOrder(10, "Hypoallergenic", NEAR_PIN, "CASH"));
        long pickup = jobId(order, "PICKUP");
        long delivery = jobId(order, "RETURN_DELIVERY");
        String rider = rider("Rider One", "rider1@example.com", true);
        at(rider, 14.605, 121.005);

        // R2 pickup: Start trip → I've arrived → Confirm pickup (weighed 11.5 kg) → Drop off at shop.
        postJson(RIDER + "/jobs/" + pickup + "/accept", rider, null).andExpect(status().isOk());
        getJson("/api/orders/" + order + "/tracking", customer)
                .andExpect(jsonPath("$.rider.name").value("Rider One"))
                .andExpect(jsonPath("$.liveTracking").value(true))
                .andExpect(jsonPath("$.riderLocation.latitude").value(14.605));
        advance(rider, pickup);
        advance(rider, pickup);
        postJson(RIDER + "/jobs/" + pickup + "/advance", rider, Map.of("weighedKg", 11.5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billableKg").value(11.50))
                .andExpect(jsonPath("$.total").value(462.50))
                .andExpect(jsonPath("$.currentActionLabel").value("Drop off at shop"));
        getJson("/api/orders/" + order + "/tracking", customer).andExpect(jsonPath("$.currentStep").value(1));
        postJson(RIDER + "/jobs/" + pickup + "/advance", rider, Map.of("weighedKg", 12))
                .andExpect(status().isBadRequest());
        postJson(RIDER + "/jobs/" + pickup + "/advance", rider, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(orderStatus(order)).isEqualTo("WASHING");

        // Return delivery is offered only once the admin marks the order Ready.
        getJson(RIDER + "/jobs/available", rider).andExpect(jsonPath("$", hasSize(0)));
        patchJson("/api/admin/orders/" + order, admin, Map.of("status", "READY")).andExpect(status().isOk());
        getJson(RIDER + "/jobs/available", rider)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].typeLabel").value("Return delivery"))
                .andExpect(jsonPath("$[0].payment.collectCash").value(true));

        // R3: Picked up at shop → Start trip → I've arrived → Cash received → Mark as delivered.
        postJson(RIDER + "/jobs/" + delivery + "/accept", rider, null).andExpect(status().isOk());
        advance(rider, delivery);
        assertThat(orderStatus(order)).isEqualTo("OUT_FOR_DELIVERY");
        advance(rider, delivery);
        postJson(RIDER + "/jobs/" + delivery + "/advance", rider, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.awaitingCash").value(true))
                .andExpect(jsonPath("$.currentActionLabel").value("Cash received"));
        postJson(RIDER + "/jobs/" + delivery + "/advance", rider, null).andExpect(status().isConflict());
        postJson(RIDER + "/jobs/" + delivery + "/cash-collected", rider, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.paid").value(true))
                .andExpect(jsonPath("$.currentActionLabel").value("Mark as delivered"));
        postJson(RIDER + "/jobs/" + delivery + "/advance", rider, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        assertThat(orderStatus(order)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT status FROM payments WHERE order_id = ?", String.class, order))
                .isEqualTo("PAID");
        assertThat(jdbc.queryForList("SELECT status FROM order_status_history WHERE order_id = ? ORDER BY id",
                String.class, order))
                .containsExactly("PENDING", "WASHING", "READY", "OUT_FOR_DELIVERY", "COMPLETED");
        getJson("/api/orders/" + order + "/tracking", customer)
                .andExpect(jsonPath("$.currentStep").value(4))
                .andExpect(jsonPath("$.timeline[4].done").value(true))
                .andExpect(jsonPath("$.liveTracking").value(false))
                .andExpect(jsonPath("$.riderLocation").doesNotExist());
        getJson(RIDER + "/summary", rider)
                .andExpect(jsonPath("$.availableCount").value(0))
                .andExpect(jsonPath("$.acceptedCount").value(0))
                .andExpect(jsonPath("$.doneTodayCount").value(2));
    }

    @Test
    void gcashDeliveryHasNoCashStep() throws Exception {
        long order = placeOrder(customer("ana@example.com"), deliveryOrder(10, "Standard", NEAR_PIN, "GCASH"));
        long delivery = jobId(order, "RETURN_DELIVERY");
        String rider = rider("Rider One", "rider1@example.com", true);
        patchJson("/api/admin/orders/" + order, admin(), Map.of("status", "READY")).andExpect(status().isOk());

        postJson(RIDER + "/jobs/" + delivery + "/accept", rider, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps", hasSize(4)))
                .andExpect(jsonPath("$.steps[3].title").value("Delivered"));
        postJson(RIDER + "/jobs/" + delivery + "/cash-collected", rider, null).andExpect(status().isConflict());
    }

    @Test
    void countersAndMyJobsFollowTheRider() throws Exception {
        String customer = customer("ana@example.com");
        long first = placeOrder(customer, deliveryOrder(10, "Standard", NEAR_PIN, "CASH"));
        placeOrder(customer, deliveryOrder(10, "Standard", FARTHER_PIN, "CASH"));
        String rider = rider("Rider One", "rider1@example.com", true);

        getJson(RIDER + "/summary", rider)
                .andExpect(jsonPath("$.riderName").value("Rider One"))
                .andExpect(jsonPath("$.availableCount").value(2))
                .andExpect(jsonPath("$.acceptedCount").value(0))
                .andExpect(jsonPath("$.doneTodayCount").value(0));
        postJson(RIDER + "/jobs/" + jobId(first, "PICKUP") + "/accept", rider, null).andExpect(status().isOk());
        getJson(RIDER + "/summary", rider)
                .andExpect(jsonPath("$.availableCount").value(1))
                .andExpect(jsonPath("$.acceptedCount").value(1));
        getJson(RIDER + "/jobs/mine", rider).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].steps[0].current").value(true));

        patchJson(RIDER + "/duty", rider, Map.of("onDuty", false)).andExpect(jsonPath("$.onDuty").value(false));
        postJson(RIDER + "/jobs/" + jobId(first, "RETURN_DELIVERY") + "/accept", rider, null)
                .andExpect(status().isConflict());
    }
}
