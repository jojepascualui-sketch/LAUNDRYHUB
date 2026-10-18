package com.laundryhub.backend.domain.order;

/** How the laundry is handed over (screen 02). Stored in {@code orders.service_type}. */
public enum ServiceCode {
    /** Customer brings the laundry to the shop. */
    WALK_IN,
    /** A rider collects it at the pinned location and returns it. */
    PICKUP_DELIVERY
}
