package com.laundryhub.backend.domain.rider;

import java.util.List;

import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.domain.payment.CashPayment;
import com.laundryhub.backend.domain.payment.PaymentMethod;
import com.laundryhub.backend.domain.user.Rider;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Return delivery: bring the clean laundry from the shop to the customer (R3).
 * Cash orders have an extra "Cash collected" step.
 */
// OOP: Inheritance — a concrete RiderJob.
@Entity
@DiscriminatorValue("RETURN_DELIVERY")
public class DeliveryJob extends RiderJob {

    /** The step that only cash orders have. */
    public static final JobStep CASH_COLLECTED = new JobStep("Cash collected", "Cash received");

    /** First step: the laundry leaves the shop, so the order is Out for delivery. */
    public static final JobStep COLLECTED_AT_SHOP = new JobStep("Collect laundry at shop", "Picked up at shop");

    /** Last step: the order is Completed. */
    public static final JobStep DELIVERED = new JobStep("Delivered", "Mark as delivered");

    /** Steps and button labels for cash orders, exactly as in DESIGN_SPEC.md (Rider app, R3). */
    public static final List<JobStep> CASH_STEPS = List.of(
            COLLECTED_AT_SHOP,
            new JobStep("On the way to customer", "Start trip"),
            new JobStep("Arrived at drop-off", "I’ve arrived"),
            CASH_COLLECTED,
            DELIVERED);

    /** Steps for orders already paid by GCash (no cash step). */
    public static final List<JobStep> PREPAID_STEPS = CASH_STEPS.stream()
            .filter(step -> !step.equals(CASH_COLLECTED))
            .toList();

    /** For JPA only. */
    protected DeliveryJob() {
    }

    /**
     * Creates a waiting return-delivery job; it is offered when the order is READY.
     *
     * @param order a Pickup &amp; Delivery order
     */
    public DeliveryJob(Order order) {
        super(order, JobStatus.WAITING);
    }

    // OOP: Polymorphism — the checklist depends on how the order is paid.
    @Override
    public List<JobStep> getSteps() {
        PaymentMethod payment = getOrder().getPayment();
        return payment != null && payment.collectsCashOnDelivery() ? CASH_STEPS : PREPAID_STEPS;
    }

    @Override
    public JobType getType() {
        return JobType.RETURN_DELIVERY;
    }

    /** A return delivery starts at the shop, where the rider collects the clean laundry. */
    @Override
    public Location getStartPoint(Location shop) {
        return shop;
    }

    /** Leaving the shop → Out for delivery; "Mark as delivered" → Completed. */
    @Override
    protected OrderStatus orderStatusAfter(JobStep step) {
        if (COLLECTED_AT_SHOP.equals(step)) {
            return OrderStatus.OUT_FOR_DELIVERY;
        }
        return DELIVERED.equals(step) ? OrderStatus.COMPLETED : null;
    }

    @Override
    public boolean isAwaitingCash() {
        return CASH_COLLECTED.equals(getCurrentStep());
    }

    /**
     * "Cash received" (R3): completes the cash step and records the cash as collected.
     *
     * @param by the assigned rider
     * @throws IllegalStateException if the current step is not the cash step
     */
    public void confirmCashCollected(Rider by) {
        if (!isAwaitingCash()) {
            throw new IllegalStateException("This job is not waiting for cash");
        }
        completeCurrentStep(by);
    }

    /** Pressing "Cash received" records the cash as collected by this rider. */
    @Override
    protected void onStepCompleted(JobStep step, Rider by) {
        if (CASH_COLLECTED.equals(step) && getOrder().getPayment() instanceof CashPayment cash && !cash.isPaid()) {
            cash.recordCollectedBy(by);
        }
    }
}
