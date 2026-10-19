package com.laundryhub.app.data.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.laundryhub.app.data.api.dto.CreateOrderRequestDto;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.PaymentOption;
import com.laundryhub.app.model.ServiceType;

import org.junit.Test;

import java.math.BigDecimal;

/** The "Place order" body: no prices, normalized GCash number, pin only for delivery. */
public class RemoteOrderRepositoryTest {

    private static final OrderDraft.Slot SLOT = new OrderDraft.Slot("2026-10-19", "2–4 PM", "Today, 2–4 PM");

    @Test
    public void deliveryGcashBody() {
        OrderDraft draft = OrderDraft.start(8, 2L).withService(ServiceType.PICKUP_DELIVERY).withKg(10)
                .withSlot(SLOT).withPin(Location.of(14.61, 121.01))
                .withAddressDetails("  Blk 1 Lot 2 ").withPayment(PaymentOption.GCASH).withGcashNumber("0917 123 4567");

        String json = new Gson().toJson(RemoteOrderRepository.toRequest(draft));

        assertEquals("{\"serviceType\":\"PICKUP_DELIVERY\",\"weightKg\":10,\"soapId\":2,"
                + "\"scheduleDate\":\"2026-10-19\",\"slotLabel\":\"2–4 PM\","
                + "\"pin\":{\"latitude\":14.610000,\"longitude\":121.010000},\"addressDetails\":\"Blk 1 Lot 2\","
                + "\"paymentMethod\":\"GCASH\",\"gcashNumber\":\"09171234567\"}", json);
    }

    @Test
    public void walkInCashSendsNoPinNoNumber() {
        OrderDraft draft = OrderDraft.start(8, 1L).withService(ServiceType.WALK_IN).withSlot(SLOT)
                .withGcashNumber("09171234567");

        CreateOrderRequestDto body = RemoteOrderRepository.toRequest(draft);

        assertEquals("CASH", body.getPaymentMethod());
        assertNull(body.getGcashNumber());
    }
}
