package com.laundryhub.app.data.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.laundryhub.app.data.api.dto.TrackingDto;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.tracking.TrackStep;
import com.laundryhub.app.model.tracking.Tracking;

import org.junit.Test;

/** JSON of GET /api/orders/{id}/tracking → the 07 model. */
public class TrackingMappingTest {

    private static final String JSON = "{\"orderId\":1,\"orderNo\":\"LH-0001\",\"status\":\"OUT_FOR_DELIVERY\","
            + "\"statusLabel\":\"Out for delivery\",\"cancelled\":false,\"currentStep\":3,\"timeline\":["
            + "{\"key\":\"ORDER_PLACED\",\"done\":true,\"current\":false},"
            + "{\"key\":\"PICKED_UP\",\"done\":true,\"current\":false},"
            + "{\"key\":\"WASHING\",\"done\":true,\"current\":false},"
            + "{\"key\":\"OUT_FOR_DELIVERY\",\"done\":false,\"current\":true},"
            + "{\"key\":\"DELIVERED\",\"done\":false,\"current\":false}],"
            + "\"rider\":{\"name\":\"Rider 1\",\"initials\":\"R1\",\"plateNumber\":\"SMP 0001\",\"mobileNumber\":null},"
            + "\"liveTracking\":true,\"riderLocation\":{\"latitude\":14.605,\"longitude\":121.015},"
            + "\"shop\":{\"latitude\":14.6,\"longitude\":121.0},\"pin\":{\"latitude\":14.6091,\"longitude\":121.0223}}";

    private static final String WALK_IN = "{\"orderId\":2,\"orderNo\":\"LH-0002\",\"statusLabel\":\"Washing\","
            + "\"cancelled\":false,\"timeline\":[{\"key\":\"ORDER_PLACED\",\"done\":true,\"current\":false}],"
            + "\"rider\":null,\"liveTracking\":false,\"riderLocation\":null,"
            + "\"shop\":{\"latitude\":14.6,\"longitude\":121.0},\"pin\":null}";

    @Test
    public void mapsTimelineRiderAndPoints() {
        Tracking t = RemoteOrderRepository.toTracking(new Gson().fromJson(JSON, TrackingDto.class));

        assertEquals("LH-0001", t.getOrderNo());
        assertEquals(5, t.getRows().size());
        assertEquals(TrackStep.OUT_FOR_DELIVERY, t.getRows().get(3).step());
        assertEquals(Tracking.State.CURRENT, t.getRows().get(3).state());
        assertEquals(Tracking.State.UPCOMING, t.getRows().get(4).state());
        assertEquals("SMP 0001", t.getRider().plate());
        assertNull(t.getRider().mobile());
        assertTrue(t.isLive());
        assertEquals(Location.of(14.6091, 121.0223), t.getPin());
    }

    @Test
    public void walkInWithoutRiderIsNotLive() {
        Tracking t = RemoteOrderRepository.toTracking(new Gson().fromJson(WALK_IN, TrackingDto.class));

        assertNull(t.getRider());
        assertFalse(t.isLive());
        assertNull(t.getPin());
    }
}
