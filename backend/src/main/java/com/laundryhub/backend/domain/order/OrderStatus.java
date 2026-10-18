package com.laundryhub.backend.domain.order;

/** Order statuses, in workflow order, with the badge text from the design. */
public enum OrderStatus {
    /** Placed, not yet washing. */
    PENDING("Pending"),
    /** Being washed and dried. */
    WASHING("Washing"),
    /** Clean and folded, waiting for pickup or return delivery. */
    READY("Ready"),
    /** A rider is bringing it back (Pickup &amp; Delivery only). */
    OUT_FOR_DELIVERY("Out for delivery"),
    /** Handed back to the customer. */
    COMPLETED("Completed"),
    /** Cancelled before completion. */
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    /** @return badge text, e.g. "Out for delivery" */
    public String getLabel() {
        return label;
    }

    /** @return whether no further status change is allowed */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * @param service the order's service
     * @return whether an order with this service can be in this status
     */
    public boolean appliesTo(ServiceCode service) {
        return this != OUT_FOR_DELIVERY || service == ServiceCode.PICKUP_DELIVERY;
    }
}
