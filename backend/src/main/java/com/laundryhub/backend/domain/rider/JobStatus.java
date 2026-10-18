package com.laundryhub.backend.domain.rider;

/** Lifecycle of a rider job. */
public enum JobStatus {
    /** Return delivery created with the order, offered once the laundry is READY. */
    WAITING,
    /** Shown to on-duty riders under "Available". */
    AVAILABLE,
    /** Accepted by a rider ("Start job"), no step done yet. */
    ACCEPTED,
    /** At least one step done ("Continue job"). */
    IN_PROGRESS,
    /** All steps done (R4 "Job completed"). */
    COMPLETED,
    /** Order cancelled. */
    CANCELLED
}
