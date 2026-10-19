package com.laundryhub.backend.domain.payment;

import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.user.Rider;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Cash: "Pay the rider upon delivery" (Pickup &amp; Delivery) or "Pay at the counter" (Walk-in).
 */
// OOP: Inheritance — a concrete PaymentMethod.
@Entity
@DiscriminatorValue("CASH")
public class CashPayment extends PaymentMethod {

    /** Method name on screen 05. */
    public static final String DISPLAY_NAME = "Cash";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by_rider_id")
    private Rider collectedBy;

    /** For JPA only. */
    protected CashPayment() {
    }

    /**
     * Creates an unpaid cash payment for the order.
     *
     * @param order the order
     */
    public CashPayment(Order order) {
        super(order);
    }

    /**
     * Records cash collected by the rider on return delivery (R3 "Cash received").
     *
     * @param rider the rider who collected it
     */
    public void recordCollectedBy(Rider rider) {
        if (!collectsCashOnDelivery()) {
            throw new IllegalStateException("Walk-in orders are paid at the counter");
        }
        this.collectedBy = Check.notNull(rider, "Rider");
        markPaid(null);
    }

    /**
     * Records cash paid at the shop counter (walk-in).
     *
     * @param receiptNo optional receipt number
     */
    public void recordPaidAtCounter(String receiptNo) {
        markPaid(receiptNo);
    }

    /** Cash: the console records cash paid at the counter; the receipt number is optional. */
    @Override
    public void confirmFromConsole(String referenceNo) {
        recordPaidAtCounter(referenceNo);
    }

    @Override
    public PaymentChoice getMethod() {
        return PaymentChoice.CASH;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    // OOP: Polymorphism — the text depends on the service, unlike GCash.
    @Override
    public String getInstructions() {
        return collectsCashOnDelivery() ? "Pay the rider upon delivery" : "Pay at the counter";
    }

    @Override
    public boolean collectsCashOnDelivery() {
        return getOrder().getServiceType().createsRiderJobs();
    }

    /** @return the rider who collected the cash, or {@code null} */
    public Rider getCollectedBy() {
        return collectedBy;
    }
}
