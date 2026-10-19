package com.laundryhub.admin.model;

import java.util.List;

/**
 * Order statuses with the design's badge colors (CLAUDE.md §2). The five design statuses are shown as
 * pipeline tiles, filter chips and status buttons; {@link #CANCELLED} has no design (§7).
 */
public enum OrderStatus {

    /** Placed, not yet washing. */
    PENDING("Pending", "status-pending"),
    /** In the machines. */
    WASHING("Washing", "status-washing"),
    /** Clean, waiting for pickup or delivery. */
    READY("Ready", "status-ready"),
    /** With a rider (delivery only). */
    OUT_FOR_DELIVERY("Out for delivery", "status-out-for-delivery"),
    /** Done. */
    COMPLETED("Completed", "status-completed"),
    /** Cancelled — no badge color in the design; shown with the Pending colors until one is chosen. */
    CANCELLED("Cancelled", "status-pending");

    /** The statuses the design shows, in workflow order. */
    public static final List<OrderStatus> DESIGN_ORDER = List.of(PENDING, WASHING, READY, OUT_FOR_DELIVERY, COMPLETED);

    // OOP: Encapsulation — fixed label and style per status.
    private final String label;
    private final String styleClass;

    OrderStatus(String label, String styleClass) {
        this.label = label;
        this.styleClass = styleClass;
    }

    /** @return text on badges and buttons, e.g. "Out for delivery" */
    public String label() {
        return label;
    }

    /** @return CSS class with the badge colors */
    public String styleClass() {
        return styleClass;
    }

    /**
     * @param apiName status name from the API, e.g. "OUT_FOR_DELIVERY"
     * @return the status, or {@link #PENDING} if unknown
     */
    public static OrderStatus fromApi(String apiName) {
        for (OrderStatus status : values()) {
            if (status.name().equals(apiName)) {
                return status;
            }
        }
        return PENDING;
    }
}
