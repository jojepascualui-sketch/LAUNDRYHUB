package com.laundryhub.admin.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void mapsApiNamesToDesignLabels() {
        assertEquals("Out for delivery", OrderStatus.fromApi("OUT_FOR_DELIVERY").label());
        assertEquals("status-ready", OrderStatus.fromApi("READY").styleClass());
        assertEquals(OrderStatus.PENDING, OrderStatus.fromApi("SOMETHING_NEW"));
    }

    @Test
    void designShowsFiveStatusesInWorkflowOrder() {
        assertEquals(5, OrderStatus.DESIGN_ORDER.size());
        assertEquals(OrderStatus.PENDING, OrderStatus.DESIGN_ORDER.get(0));
        assertFalse(OrderStatus.DESIGN_ORDER.contains(OrderStatus.CANCELLED));
    }
}
