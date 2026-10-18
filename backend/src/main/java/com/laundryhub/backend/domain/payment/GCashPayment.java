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

    @Column(name = "gcash_number", length = 11)
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
        super(order);
        this.gcashNumber = Check.mobileNumber(gcashNumber, "GCash mobile number");
    }

    /**
     * Records a confirmed GCash transfer.
     *
     * @param referenceNo GCash reference number
     */
    public void confirm(String referenceNo) {
        markPaid(Check.text(referenceNo, "GCash reference number", MAX_REFERENCE_LENGTH));
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
