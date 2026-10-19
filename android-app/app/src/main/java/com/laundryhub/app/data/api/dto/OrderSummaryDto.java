package com.laundryhub.app.data.api.dto;

/** One row of {@code GET /api/orders/mine} (newest first). Filled by Gson. */
public final class OrderSummaryDto {

    // OOP: Encapsulation — read-only fields set by Gson.
    private Long id;
    private String orderNo;
    private String statusLabel;

    /** For Gson. */
    public OrderSummaryDto() {
    }

    /** @return order id */
    public Long getId() {
        return id;
    }

    /** @return "LH-0001" */
    public String getOrderNo() {
        return orderNo;
    }

    /** @return "Out for delivery" */
    public String getStatusLabel() {
        return statusLabel;
    }
}
