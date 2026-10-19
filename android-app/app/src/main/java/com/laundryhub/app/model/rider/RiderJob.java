package com.laundryhub.app.model.rider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.PaymentOption;

import java.math.BigDecimal;
import java.util.List;

/**
 * A booking (R1 card) or active job (R2/R3/R4) as served by {@code /api/rider/jobs}. Immutable.
 */
public final class RiderJob {

    /** Pickup: once "Laundry picked up & weighed" (the third step) is done, the rider heads to the shop. */
    private static final int PICKUP_COLLECTED_STEP = 3;

    /** Status values used by the app. */
    public enum Status {
        /** Offered to on-duty riders. */
        AVAILABLE,
        /** Accepted, no step done yet. */
        ACCEPTED,
        /** At least one step done. */
        IN_PROGRESS,
        /** All steps done (R4). */
        COMPLETED,
        /** Any other server status (waiting, cancelled). */
        OTHER
    }

    // OOP: Encapsulation — private final fields, validated in the constructor.
    private final long id;
    private final String orderNo;
    private final JobType type;
    private final Status status;
    @Nullable
    private final BigDecimal distanceKm;
    private final String customerName;
    private final String customerInitials;
    @Nullable
    private final String customerMobile;
    private final String address;
    @Nullable
    private final Location customerPin;
    @Nullable
    private final Location shop;
    private final String scheduleText;
    private final BigDecimal billableKg;
    private final BigDecimal total;
    private final PaymentOption paymentMethod;
    private final List<JobStep> steps;
    private final int currentStepIndex;
    private final boolean awaitingCash;

    /**
     * @param id               job id
     * @param orderNo          "LH-0003"
     * @param type             pickup or return delivery
     * @param status           job status
     * @param distanceKm       distance to the first stop, or {@code null}
     * @param customerName     customer
     * @param customerInitials avatar initials
     * @param customerMobile   phone for the call button, or {@code null} before accepting
     * @param address          address line ("Brgy. Poblacion · [STREET]")
     * @param customerPin      customer pin, or {@code null}
     * @param shop             shop position, or {@code null}
     * @param scheduleText     "Today, 4–6 PM"
     * @param billableKg       kilos
     * @param total            order total
     * @param paymentMethod    Cash or GCash
     * @param steps            checklist in order
     * @param currentStepIndex index of the next step (= steps done)
     * @param awaitingCash     whether the next step is "Cash collected"
     * @throws IllegalArgumentException if a required value is missing
     */
    public RiderJob(long id, @NonNull String orderNo, @NonNull JobType type, @NonNull Status status,
                    @Nullable BigDecimal distanceKm, @NonNull String customerName, @NonNull String customerInitials,
                    @Nullable String customerMobile, @NonNull String address, @Nullable Location customerPin,
                    @Nullable Location shop, @NonNull String scheduleText, @NonNull BigDecimal billableKg,
                    @NonNull BigDecimal total, @NonNull PaymentOption paymentMethod, @NonNull List<JobStep> steps,
                    int currentStepIndex, boolean awaitingCash) {
        if (orderNo == null || type == null || status == null || customerName == null || billableKg == null
                || total == null || paymentMethod == null || steps == null) {
            throw new IllegalArgumentException("Incomplete job");
        }
        if (currentStepIndex < 0 || currentStepIndex > steps.size()) {
            throw new IllegalArgumentException("Step index out of range");
        }
        this.id = id;
        this.orderNo = orderNo;
        this.type = type;
        this.status = status;
        this.distanceKm = distanceKm;
        this.customerName = customerName;
        this.customerInitials = customerInitials == null ? "" : customerInitials;
        this.customerMobile = customerMobile;
        this.address = address == null ? "" : address;
        this.customerPin = customerPin;
        this.shop = shop;
        this.scheduleText = scheduleText == null ? "" : scheduleText;
        this.billableKg = billableKg;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.steps = List.copyOf(steps);
        this.currentStepIndex = currentStepIndex;
        this.awaitingCash = awaitingCash;
    }

    /** @return whether all steps are done (R4) */
    public boolean isCompleted() {
        return status == Status.COMPLETED || currentStepIndex >= steps.size();
    }

    /** @return the big button text, or {@code null} when completed */
    @Nullable
    public String currentActionLabel() {
        return isCompleted() ? null : steps.get(currentStepIndex).actionLabel();
    }

    /** @return whether any step is done ("Continue job" instead of "Start job") */
    public boolean isStarted() {
        return currentStepIndex > 0;
    }

    /**
     * Where to drive now: the first stop until the rider has left it (pickup: customer then shop;
     * return delivery: shop then customer).
     *
     * @return the next stop, or {@code null} if unknown
     */
    @Nullable
    public Location nextStop() {
        if (isCompleted()) {
            return null;
        }
        // Pickup: customer, then shop. Return delivery: shop, then customer.
        boolean firstLegDone = type == JobType.PICKUP ? currentStepIndex >= PICKUP_COLLECTED_STEP : currentStepIndex > 0;
        Location first = type == JobType.PICKUP ? customerPin : shop;
        Location second = type == JobType.PICKUP ? shop : customerPin;
        return firstLegDone ? second : first;
    }

    /** @return job id */
    public long getId() {
        return id;
    }

    /** @return "LH-0003" */
    @NonNull
    public String getOrderNo() {
        return orderNo;
    }

    /** @return job type */
    @NonNull
    public JobType getType() {
        return type;
    }

    /** @return status */
    @NonNull
    public Status getStatus() {
        return status;
    }

    /** @return distance in km, or {@code null} */
    @Nullable
    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    /** @return customer name */
    @NonNull
    public String getCustomerName() {
        return customerName;
    }

    /** @return avatar initials */
    @NonNull
    public String getCustomerInitials() {
        return customerInitials;
    }

    /** @return phone number, or {@code null} */
    @Nullable
    public String getCustomerMobile() {
        return customerMobile;
    }

    /** @return address line */
    @NonNull
    public String getAddress() {
        return address;
    }

    /** @return customer pin, or {@code null} */
    @Nullable
    public Location getCustomerPin() {
        return customerPin;
    }

    /** @return shop position, or {@code null} */
    @Nullable
    public Location getShop() {
        return shop;
    }

    /** @return "Today, 4–6 PM" */
    @NonNull
    public String getScheduleText() {
        return scheduleText;
    }

    /** @return kilos */
    @NonNull
    public BigDecimal getBillableKg() {
        return billableKg;
    }

    /** @return order total */
    @NonNull
    public BigDecimal getTotal() {
        return total;
    }

    /** @return Cash or GCash */
    @NonNull
    public PaymentOption getPaymentMethod() {
        return paymentMethod;
    }

    /** @return checklist (read-only) */
    @NonNull
    public List<JobStep> getSteps() {
        return steps;
    }

    /** @return index of the next step */
    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    /** @return whether the next step is "Cash collected" */
    public boolean isAwaitingCash() {
        return awaitingCash;
    }
}
