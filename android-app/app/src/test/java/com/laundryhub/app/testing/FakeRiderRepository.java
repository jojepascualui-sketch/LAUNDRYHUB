package com.laundryhub.app.testing;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.rider.RiderRepository;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.PaymentOption;
import com.laundryhub.app.model.rider.JobStep;
import com.laundryhub.app.model.rider.JobType;
import com.laundryhub.app.model.rider.RiderJob;
import com.laundryhub.app.model.rider.RiderSummary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * In-memory rider backend with the four bookings of source/Rider.dc.html (LH-0003, LH-0008,
 * LH-0006, LH-0001). Answers synchronously and follows the server's rules: off duty → no
 * available jobs, accept of a taken job → 409, steps advance one at a time.
 */
public class FakeRiderRepository implements RiderRepository {

    /** Job ids of the design's bookings. */
    public static final long ANA = 3, CARLO = 8, PAOLO = 6, MARIA = 1;

    public static final List<JobStep> PICKUP_STEPS = List.of(
            new JobStep("Head to customer", "Start trip"),
            new JobStep("Arrived at pickup point", "I’ve arrived"),
            new JobStep("Laundry picked up & weighed", "Confirm pickup"),
            new JobStep("Dropped off at shop", "Drop off at shop"));

    public static final List<JobStep> DELIVERY_CASH_STEPS = List.of(
            new JobStep("Collect laundry at shop", "Picked up at shop"),
            new JobStep("On the way to customer", "Start trip"),
            new JobStep("Arrived at drop-off", "I’ve arrived"),
            new JobStep("Cash collected", "Cash received"),
            new JobStep("Delivered", "Mark as delivered"));

    public static final List<JobStep> DELIVERY_GCASH_STEPS = List.of(
            new JobStep("Collect laundry at shop", "Picked up at shop"),
            new JobStep("On the way to customer", "Start trip"),
            new JobStep("Arrived at drop-off", "I’ve arrived"),
            new JobStep("Delivered", "Mark as delivered"));

    /** Index of "Cash collected" in {@link #DELIVERY_CASH_STEPS}. */
    public static final int CASH_STEP = 3;

    public static final Location SHOP = Location.of(14.5995, 120.9842);

    public String riderName = "[RIDER NAME]";
    public boolean onDuty = true;
    public long doneToday;
    public final List<RiderJob> available = new ArrayList<>();
    public final List<RiderJob> mine = new ArrayList<>();
    public final List<RiderJob> completed = new ArrayList<>();
    /** Jobs another rider "wins" first: accept answers 409. */
    public final Set<Long> takenByOthers = new HashSet<>();
    public final List<Location> sentLocations = new ArrayList<>();
    /** Endpoint the last completeStep would call: "advance" or "cash-collected". */
    public String lastStepEndpoint;
    /** When set, every call fails with this status. */
    public Integer failWith;

    public FakeRiderRepository() {
        available.add(job(ANA, "LH-0003", JobType.PICKUP, "1.2", "Ana Reyes", "AR", "Brgy. Poblacion · [STREET]",
                "Today, 4–6 PM", "12", "516.00", PaymentOption.CASH, 14.6040, 120.9900));
        available.add(job(CARLO, "LH-0008", JobType.RETURN_DELIVERY, "2.1", "Carlo Mendoza", "CM",
                "Brgy. Mabini · [STREET]", "Today, 4–6 PM", "9", "330.00", PaymentOption.CASH, 14.6120, 120.9950));
        available.add(job(PAOLO, "LH-0006", JobType.PICKUP, "2.8", "Paolo Tan", "PT", "Brgy. Bagong Silang · [STREET]",
                "Tomorrow, 9–11 AM", "15", "585.00", PaymentOption.CASH, 14.6200, 120.9700));
        available.add(job(MARIA, "LH-0001", JobType.RETURN_DELIVERY, "1.9", "Maria Santos", "MS",
                "Brgy. San Isidro · [STREET]", "Today, 2–4 PM", "10", "410.00", PaymentOption.GCASH, 14.5900, 121.0000));
    }

    private static RiderJob job(long id, String no, JobType type, String km, String name, String initials,
                                String address, String slot, String kg, String total, PaymentOption pay,
                                double lat, double lng) {
        return new RiderJob(id, no, type, RiderJob.Status.AVAILABLE, new BigDecimal(km), name, initials, null,
                address, Location.of(lat, lng), SHOP, slot, new BigDecimal(kg), new BigDecimal(total), pay,
                stepsFor(type, pay), 0, false);
    }

    /**
     * @param type job type
     * @param pay  payment
     * @return the design's checklist
     */
    public static List<JobStep> stepsFor(JobType type, PaymentOption pay) {
        if (type == JobType.PICKUP) {
            return PICKUP_STEPS;
        }
        return pay == PaymentOption.CASH ? DELIVERY_CASH_STEPS : DELIVERY_GCASH_STEPS;
    }

    /**
     * Accepts a booking and completes {@code steps} steps (screenshot set-up).
     *
     * @param id    job id
     * @param steps steps already done
     * @return the job in that state
     */
    public RiderJob acceptAt(long id, int steps) {
        RiderJob job = find(available, id);
        available.remove(job);
        RiderJob moved = at(job, steps);
        if (moved.isCompleted()) {
            completed.add(moved);
            doneToday++;
        } else {
            mine.add(moved);
        }
        return moved;
    }

    private static RiderJob at(RiderJob job, int steps) {
        int size = job.getSteps().size();
        int index = Math.min(steps, size);
        RiderJob.Status status = index >= size ? RiderJob.Status.COMPLETED
                : index == 0 ? RiderJob.Status.ACCEPTED : RiderJob.Status.IN_PROGRESS;
        boolean cash = job.getType().collectsCash(job.getPaymentMethod()) && index == CASH_STEP;
        return new RiderJob(job.getId(), job.getOrderNo(), job.getType(), status, job.getDistanceKm(),
                job.getCustomerName(), job.getCustomerInitials(), "09171234567", job.getAddress(),
                job.getCustomerPin(), job.getShop(), job.getScheduleText(), job.getBillableKg(), job.getTotal(),
                job.getPaymentMethod(), job.getSteps(), index, cash);
    }

    private static RiderJob find(List<RiderJob> jobs, long id) {
        for (RiderJob job : jobs) {
            if (job.getId() == id) {
                return job;
            }
        }
        return null;
    }

    private boolean failed(ApiCallback<?> callback) {
        if (failWith != null) {
            callback.onError(new ApiException(failWith, "Server error " + failWith, null));
            return true;
        }
        return false;
    }

    private RiderSummary summaryNow() {
        return new RiderSummary(riderName, onDuty, available.size(), mine.size(), doneToday);
    }

    @Override
    public void summary(@NonNull ApiCallback<RiderSummary> callback) {
        if (!failed(callback)) {
            callback.onSuccess(summaryNow());
        }
    }

    @Override
    public void setDuty(boolean duty, @NonNull ApiCallback<RiderSummary> callback) {
        if (!failed(callback)) {
            onDuty = duty;
            callback.onSuccess(summaryNow());
        }
    }

    @Override
    public void available(@NonNull ApiCallback<List<RiderJob>> callback) {
        if (!failed(callback)) {
            callback.onSuccess(onDuty ? List.copyOf(available) : List.of());
        }
    }

    @Override
    public void mine(@NonNull ApiCallback<List<RiderJob>> callback) {
        if (!failed(callback)) {
            callback.onSuccess(List.copyOf(mine));
        }
    }

    @Override
    public void job(long jobId, @NonNull ApiCallback<RiderJob> callback) {
        if (failed(callback)) {
            return;
        }
        RiderJob job = find(mine, jobId);
        if (job == null) {
            job = find(completed, jobId);
        }
        if (job == null) {
            callback.onError(new ApiException(404, "Job not found", null));
        } else {
            callback.onSuccess(job);
        }
    }

    @Override
    public void accept(long jobId, @NonNull ApiCallback<RiderJob> callback) {
        if (failed(callback)) {
            return;
        }
        if (takenByOthers.contains(jobId)) {
            available.remove(find(available, jobId));
            callback.onError(new ApiException(ALREADY_TAKEN, "This booking was already taken by another rider.", null));
            return;
        }
        callback.onSuccess(acceptAt(jobId, 0));
    }

    @Override
    public void decline(long jobId, @NonNull ApiCallback<Boolean> callback) {
        if (!failed(callback)) {
            available.remove(find(available, jobId));
            callback.onSuccess(true);
        }
    }

    @Override
    public void completeStep(@NonNull RiderJob job, @NonNull ApiCallback<RiderJob> callback) {
        if (failed(callback)) {
            return;
        }
        lastStepEndpoint = job.isAwaitingCash() ? "cash-collected" : "advance";
        RiderJob current = find(mine, job.getId());
        mine.remove(current);
        RiderJob next = at(current, current.getCurrentStepIndex() + 1);
        if (next.isCompleted()) {
            completed.add(next);
            doneToday++;
        } else {
            mine.add(next);
        }
        callback.onSuccess(next);
    }

    @Override
    public void sendLocation(@NonNull Location location, BigDecimal accuracyMeters,
                             @NonNull ApiCallback<Boolean> callback) {
        if (!failed(callback)) {
            sentLocations.add(location);
            callback.onSuccess(true);
        }
    }
}
