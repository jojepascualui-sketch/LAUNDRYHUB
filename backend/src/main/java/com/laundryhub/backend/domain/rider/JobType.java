package com.laundryhub.backend.domain.rider;

/** Kind of rider job, stored in {@code rider_jobs.job_type}, with the badge text from R1. */
public enum JobType {
    /** Customer → shop. Blue "Pickup" badge. */
    PICKUP("Pickup"),
    /** Shop → customer. Orange "Return delivery" badge. */
    RETURN_DELIVERY("Return delivery");

    private final String label;

    JobType(String label) {
        this.label = label;
    }

    /** @return badge text */
    public String getLabel() {
        return label;
    }
}
