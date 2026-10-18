package com.laundryhub.backend.web.dto;

import com.laundryhub.backend.domain.rider.JobStatus;
import com.laundryhub.backend.domain.rider.JobType;
import com.laundryhub.backend.domain.rider.RiderJob;

/**
 * A rider job of an order, for A2 "Assign rider".
 *
 * @param id               job id
 * @param type             PICKUP or RETURN_DELIVERY
 * @param typeLabel        "Pickup" / "Return delivery"
 * @param status           job status
 * @param riderId          assigned rider, or {@code null}
 * @param riderName        assigned rider name, or {@code null}
 * @param currentStepIndex steps done
 * @param stepCount        number of steps
 * @param currentStep      title of the next step, or {@code null} when completed
 */
public record AdminJobResponse(Long id, JobType type, String typeLabel, JobStatus status, Long riderId,
                               String riderName, int currentStepIndex, int stepCount, String currentStep) {

    /**
     * @param job a job
     * @return the DTO
     */
    public static AdminJobResponse from(RiderJob job) {
        return new AdminJobResponse(job.getId(), job.getType(), job.getType().getLabel(), job.getStatus(),
                job.getRider() == null ? null : job.getRider().getId(),
                job.getRider() == null ? null : job.getRider().getFullName(), job.getCurrentStepIndex(),
                job.getSteps().size(), job.getCurrentStep() == null ? null : job.getCurrentStep().title());
    }
}
