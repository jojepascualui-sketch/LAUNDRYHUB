package com.laundryhub.backend.domain.payment;

import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.order.Order;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** GCash: "Pay instantly from your e-wallet", with the customer's "GCash mobile number". */
// OOP: Inheritance — a concrete PaymentMethod.
@Entity
@DiscriminatorValue("GCASH")
public class GCashPayment extends PaymentMethod {

    /** Method name on screen 05. */
    public static final String DISPLAY_NAME = "GCash";

    @Column(name = "gcash_number", length = Check.MOBILE_LENGTH)
    private String gcashNumber;

    /** For JPA only. */
    protected GCashPayment() {
    }

    /**
     * Creates an unpaid GCash payment for the order.
     *
     * @param order       the order
     * @param gcashNumber customer's GCash number, 09XXXXXXXXX
     */
    public GCashPayment(Order order, String gcashNumber) {
        // The number is checked before super() links this payment to the order.
        super(withValidNumber(order, gcashNumber));
        this.gcashNumber = Check.mobileNumber(gcashNumber, "GCash mobile number");
    }

    private static Order withValidNumber(Order order, String gcashNumber) {
        Check.mobileNumber(gcashNumber, "GCash mobile number");
        return order;
    }

    /**
     * Records a confirmed GCash transfer.
     *
     * @param referenceNo GCash reference number
     */
    public void confirm(String referenceNo) {
        markPaid(Check.text(referenceNo, "GCash reference number", MAX_REFERENCE_LENGTH));
    }

    /** GCash: the console confirms the transfer; the GCash reference is required. */
    @Override
    public void confirmFromConsole(String referenceNo) {
        confirm(referenceNo);
    }

    @Override
    public PaymentChoice getMethod() {
        return PaymentChoice.GCASH;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    // OOP: Polymorphism — GCash shows the same text for every service.
    @Override
    public String getInstructions() {
        return "Pay instantly from your e-wallet";
    }

    @Override
    public boolean collectsCashOnDelivery() {
        return false;
    }

    /** @return customer's GCash number */
    public String getGcashNumber() {
        return gcashNumber;
    }
}
