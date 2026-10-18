package com.laundryhub.backend.domain.payment;

/** Payment state, shown as "(paid)" / "(unpaid)" on A2. */
public enum PaymentStatus {
    /** Not received yet. */
    UNPAID,
    /** Received. */
    PAID,
    /** Returned to the customer, e.g. after a cancellation. */
    REFUNDED
}
