package com.laundryhub.backend.domain.rider;

import java.time.LocalDateTime;
import java.util.List;

import com.laundryhub.backend.domain.common.AuditedEntity;
import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.domain.user.Rider;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A booking that on-duty riders can accept or decline (R1) and then work through step by step
 * (R2/R3). Every Pickup &amp; Delivery order has a {@link PickupJob} and a {@link DeliveryJob}.
 */
// OOP: Abstraction — the accept/decline/step workflow is shared; each job type supplies its steps.
// OOP: Inheritance — PickupJob and DeliveryJob extend this class.
@Entity
@Table(name = "rider_jobs")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "job_type", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class RiderJob extends AuditedEntity {

    // OOP: Encapsulation — status, rider and progress change only through accept/assignTo/completeCurrentStep.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id")
    private Rider rider;

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @Column(name = "available_at")
    private LocalDateTime availableAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Stops two riders from accepting the same job at the same time. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** For JPA only. */
    protected RiderJob() {
    }

    /**
     * Creates a job for a Pickup &amp; Delivery order.
     *
     * @param order         the order
     * @param initialStatus {@link JobStatus#AVAILABLE} or {@link JobStatus#WAITING}
     */
    protected RiderJob(Order order, JobStatus initialStatus) {
        this.order = Check.notNull(order, "Order");
        if (!order.getServiceType().createsRiderJobs()) {
            throw new IllegalArgumentException("Walk-in orders have no rider jobs");
        }
        this.status = initialStatus;
        if (initialStatus == JobStatus.AVAILABLE) {
            availableAt = LocalDateTime.now();
        }
    }

    /**
     * Creates the two jobs of a Pickup &amp; Delivery order: an available pickup and a waiting
     * return delivery.
     *
     * @param order the order
     * @return both jobs, or an empty list for walk-in
     */
    public static List<RiderJob> createFor(Order order) {
        Check.notNull(order, "Order");
        if (!order.getServiceType().createsRiderJobs()) {
            return List.of();
        }
        return List.of(new PickupJob(order), new DeliveryJob(order));
    }

    /** @return the checklist for this job, in order */
    // OOP: Polymorphism — PickupJob and DeliveryJob return different steps.
    public abstract List<JobStep> getSteps();

    /** @return pickup or return delivery */
    public abstract JobType getType();

    /**
     * Where the rider has to go first: the customer's pin for a pickup, the shop for a return
     * delivery. Used for the "· 1.2 km" distance on R1 and to sort the available list.
     *
     * @param shop the shop position, may be {@code null} if it is not set
     * @return the first stop, or {@code null} if unknown
     */
    // OOP: Polymorphism — each job type knows its own first stop.
    public abstract Location getStartPoint(Location shop);

    /**
     * The order status a finished step moves the order to, e.g. "Dropped off at shop" → Washing.
     *
     * @param step the step just completed
     * @return the new order status, or {@code null} if the step does not change the order
     */
    // OOP: Polymorphism — PickupJob and DeliveryJob move the order at different steps.
    protected abstract OrderStatus orderStatusAfter(JobStep step);

    /** @return whether the current step is the cash step, which needs an explicit "Cash received" */
    // OOP: Polymorphism — only a cash return delivery overrides this.
    public boolean isAwaitingCash() {
        return false;
    }

    /**
     * Called after a step is done; subclasses react to specific steps.
     *
     * @param step the step just completed
     * @param by   the rider who completed it
     */
    protected void onStepCompleted(JobStep step, Rider by) {
        // Most steps need no extra action.
    }

    /**
     * Offers a waiting job to riders.
     *
     * @throws IllegalStateException if the job is not waiting
     */
    public void offer() {
        requireStatus(JobStatus.WAITING);
        availableAt = LocalDateTime.now();
        if (rider != null) {
            // An admin assigned a rider in advance (A2 "Assign rider"): it goes straight to them.
            status = JobStatus.ACCEPTED;
            acceptedAt = availableAt;
        } else {
            status = JobStatus.AVAILABLE;
        }
    }

    /**
     * A2 "Assign rider": an admin gives the job to a rider, or moves it to another rider.
     * Progress already made is kept. A waiting job keeps waiting and goes to this rider when offered.
     *
     * @param to the rider who gets the job
     * @throws IllegalStateException if the job is already completed or cancelled
     */
    public void assignTo(Rider to) {
        Check.notNull(to, "Rider");
        if (status == JobStatus.COMPLETED || status == JobStatus.CANCELLED) {
            throw new IllegalStateException("Job is " + status + " and cannot be assigned");
        }
        rider = to;
        if (status == JobStatus.AVAILABLE) {
            status = JobStatus.ACCEPTED;
        }
        if (status != JobStatus.WAITING) {
            acceptedAt = LocalDateTime.now();
        }
    }

    /**
     * "Accept booking": assigns the job to an on-duty rider.
     *
     * @param by the rider accepting
     * @throws IllegalStateException if the job is no longer available or the rider is off duty
     */
    public void accept(Rider by) {
        Check.notNull(by, "Rider");
        requireStatus(JobStatus.AVAILABLE);
        if (!by.isOnDuty()) {
            throw new IllegalStateException("Go on duty to accept bookings");
        }
        rider = by;
        status = JobStatus.ACCEPTED;
        acceptedAt = LocalDateTime.now();
    }

    /**
     * "Decline": hides the job from this rider only.
     *
     * @param by the rider declining
     * @return the decline to save
     * @throws IllegalStateException if the job is no longer available
     */
    public RiderJobDecline decline(Rider by) {
        requireStatus(JobStatus.AVAILABLE);
        return new RiderJobDecline(this, Check.notNull(by, "Rider"));
    }

    /**
     * Presses the big button: completes the current step; the last step completes the job.
     *
     * @param by the assigned rider
     * @throws IllegalStateException if the job is not active or belongs to another rider
     */
    public void completeCurrentStep(Rider by) {
        if (status != JobStatus.ACCEPTED && status != JobStatus.IN_PROGRESS) {
            throw new IllegalStateException("Job is " + status + ", not active");
        }
        if (!isAssignedTo(by)) {
            throw new IllegalStateException("Only the assigned rider can update this job");
        }
        JobStep step = getCurrentStep();
        currentStep++;
        status = currentStep == getSteps().size() ? JobStatus.COMPLETED : JobStatus.IN_PROGRESS;
        if (status == JobStatus.COMPLETED) {
            completedAt = LocalDateTime.now();
        }
        onStepCompleted(step, by);
        OrderStatus reached = orderStatusAfter(step);
        if (reached != null) {
            // Writes order_status_history through Order.changeStatus.
            order.changeStatus(reached, by, getType().getLabel() + ": " + step.title());
        }
    }

    /** Cancels the job because its order was cancelled. */
    public void cancel() {
        if (status != JobStatus.COMPLETED) {
            status = JobStatus.CANCELLED;
        }
    }

    /**
     * @param candidate a rider
     * @return whether the job is assigned to that rider
     */
    public boolean isAssignedTo(Rider candidate) {
        if (rider == null || candidate == null) {
            return false;
        }
        return rider == candidate || (rider.getId() != null && rider.getId().equals(candidate.getId()));
    }

    private void requireStatus(JobStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Job is " + status + ", expected " + expected);
        }
    }

    /** @return the step to do next, or {@code null} when the job is completed */
    public JobStep getCurrentStep() {
        List<JobStep> steps = getSteps();
        return currentStep < steps.size() ? steps.get(currentStep) : null;
    }

    /** @return whether a rider is working on the job (accepted or in progress) */
    public boolean isActive() {
        return status == JobStatus.ACCEPTED || status == JobStatus.IN_PROGRESS;
    }

    /** @return the order */
    public Order getOrder() {
        return order;
    }

    /** @return job status */
    public JobStatus getStatus() {
        return status;
    }

    /** @return the assigned rider, or {@code null} */
    public Rider getRider() {
        return rider;
    }

    /** @return index of the step to do next, which is also the number of steps done */
    public int getCurrentStepIndex() {
        return currentStep;
    }

    /** @return when the job was offered, or {@code null} */
    public LocalDateTime getAvailableAt() {
        return availableAt;
    }

    /** @return when it was accepted, or {@code null} */
    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    /** @return when it was completed, or {@code null} */
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
