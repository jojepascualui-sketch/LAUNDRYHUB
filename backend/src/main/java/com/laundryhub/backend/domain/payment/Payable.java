package com.laundryhub.backend.domain.payment;

import java.math.BigDecimal;

/** Something the customer pays for an order (screen 05 "Payment method"). */
// OOP: Abstraction — screens and rider jobs use any payment through this interface.
public interface Payable {

    /** @return what is still owed: the amount, or ₱0.00 once paid */
    BigDecimal getAmountDue();

    /** @return whether the payment has been received */
    boolean isPaid();

    /** @return method name, e.g. "Cash" or "GCash" */
    String getDisplayName();

    /** @return the line under the method name, e.g. "Pay the rider upon delivery" */
    String getInstructions();

    /** @return whether the rider collects the money on return delivery ("Cash collected" step) */
    boolean collectsCashOnDelivery();
}
