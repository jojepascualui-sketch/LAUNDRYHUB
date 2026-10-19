package com.laundryhub.app.data.rider;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.rider.RiderJob;
import com.laundryhub.app.model.rider.RiderSummary;

import java.math.BigDecimal;
import java.util.List;

/** The rider app (R1–R5) and the on-duty GPS service. */
// OOP: Abstraction — ViewModels and the service depend on this interface; tests use a fake.
public interface RiderRepository {

    /** HTTP status the server sends when another rider accepted the booking first. */
    int ALREADY_TAKEN = 409;

    /**
     * @param callback header and counters
     */
    void summary(@NonNull ApiCallback<RiderSummary> callback);

    /**
     * @param onDuty   new switch position
     * @param callback updated header
     */
    void setDuty(boolean onDuty, @NonNull ApiCallback<RiderSummary> callback);

    /**
     * @param callback "Available" bookings, nearest first (empty when off duty)
     */
    void available(@NonNull ApiCallback<List<RiderJob>> callback);

    /**
     * @param callback "My jobs"
     */
    void mine(@NonNull ApiCallback<List<RiderJob>> callback);

    /**
     * @param jobId    job id
     * @param callback the job
     */
    void job(long jobId, @NonNull ApiCallback<RiderJob> callback);

    /**
     * @param jobId    job id
     * @param callback the accepted job; error status {@link #ALREADY_TAKEN} if someone was first
     */
    void accept(long jobId, @NonNull ApiCallback<RiderJob> callback);

    /**
     * @param jobId    job id
     * @param callback done when hidden
     */
    void decline(long jobId, @NonNull ApiCallback<Boolean> callback);

    /**
     * The big button: "Cash received" on the cash step, otherwise the next step.
     *
     * @param job      the current job
     * @param callback the job after the step
     */
    void completeStep(@NonNull RiderJob job, @NonNull ApiCallback<RiderJob> callback);

    /**
     * @param location       GPS fix
     * @param accuracyMeters accuracy, or {@code null}
     * @param callback       done
     */
    void sendLocation(@NonNull Location location, BigDecimal accuracyMeters, @NonNull ApiCallback<Boolean> callback);
}
