package com.laundryhub.app.model.tracking;

import androidx.annotation.NonNull;

/**
 * One of the customer's orders (Orders tab → Track order).
 *
 * @param id          order id
 * @param orderNo     "LH-0001"
 * @param statusLabel "Washing"
 */
public record OrderSummary(long id, @NonNull String orderNo, @NonNull String statusLabel) {

    /** Validates the order number. */
    public OrderSummary {
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("Order number is required");
        }
        if (statusLabel == null) {
            statusLabel = "";
        }
    }
}
