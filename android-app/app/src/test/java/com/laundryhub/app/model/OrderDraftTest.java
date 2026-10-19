package com.laundryhub.app.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class OrderDraftTest {

    private static final OrderDraft.Slot SLOT = new OrderDraft.Slot("2026-10-19", "morning", "Today, morning");

    @Test
    public void startsLikeThePrototype() {
        OrderDraft draft = OrderDraft.start(8, 1L);

        assertNull(draft.getService());
        assertEquals(8, draft.getKg());
        assertEquals(Long.valueOf(1L), draft.getSoapId());
        assertNull(draft.getSlot());
        assertEquals(PaymentOption.CASH, draft.getPayment());
    }

    @Test
    public void changingTheServiceClearsTheSlot() {
        OrderDraft walkIn = OrderDraft.start(8, 1L).withService(ServiceType.WALK_IN).withSlot(SLOT);

        assertNotNull(walkIn.withService(ServiceType.WALK_IN).getSlot());
        assertNull(walkIn.withService(ServiceType.PICKUP_DELIVERY).getSlot());
    }

    @Test(expected = IllegalArgumentException.class)
    public void weightMustBePositive() {
        OrderDraft.start(8, 1L).withKg(0);
    }
}
