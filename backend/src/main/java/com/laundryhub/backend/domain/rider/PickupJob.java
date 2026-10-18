package com.laundryhub.backend.domain.rider;

import java.util.List;

import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Pickup: collect the laundry at the customer's pin and bring it to the shop (R2). */
// OOP: Inheritance — a concrete RiderJob.
@Entity
@DiscriminatorValue("PICKUP")
public class PickupJob extends RiderJob {

    /** "Confirm pickup": the rider may send the weighed kilos with this step. */
    public static final JobStep PICKED_UP_AND_WEIGHED = new JobStep("Laundry picked up & weighed", "Confirm pickup");

    /** Last step: the laundry is at the shop, so the order moves to Washing. */
    public static final JobStep DROPPED_OFF = new JobStep("Dropped off at shop", "Drop off at shop");

    /** Steps and button labels exactly as in DESIGN_SPEC.md (Rider app, R2). */
    public static final List<JobStep> STEPS = List.of(
            new JobStep("Head to customer", "Start trip"),
            new JobStep("Arrived at pickup point", "I’ve arrived"),
            PICKED_UP_AND_WEIGHED,
            DROPPED_OFF);

    /** For JPA only. */
    protected PickupJob() {
    }

    /**
     * Creates an available pickup job.
     *
     * @param order a Pickup &amp; Delivery order
     */
    public PickupJob(Order order) {
        super(order, JobStatus.AVAILABLE);
    }

    // OOP: Polymorphism — pickup has its own fixed checklist.
    @Override
    public List<JobStep> getSteps() {
        return STEPS;
    }

    @Override
    public JobType getType() {
        return JobType.PICKUP;
    }

    /** A pickup starts at the customer's pin. */
    @Override
    public Location getStartPoint(Location shop) {
        return getOrder().getPickupLocation();
    }

    /** "Dropped off at shop" puts the order into Washing. */
    @Override
    protected OrderStatus orderStatusAfter(JobStep step) {
        return DROPPED_OFF.equals(step) ? OrderStatus.WASHING : null;
    }

    /** @return whether the laundry has been collected from the customer (07 "Picked up") */
    public boolean isLaundryCollected() {
        return getCurrentStepIndex() > STEPS.indexOf(PICKED_UP_AND_WEIGHED);
    }

    /** @return whether the rider is about to press "Confirm pickup" (weighing) */
    public boolean isAtWeighingStep() {
        return PICKED_UP_AND_WEIGHED.equals(getCurrentStep());
    }
}
