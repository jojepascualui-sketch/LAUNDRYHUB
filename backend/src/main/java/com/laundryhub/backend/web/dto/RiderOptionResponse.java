package com.laundryhub.backend.web.dto;

import com.laundryhub.backend.domain.user.Rider;

/**
 * An entry of the A2 "Assign rider" dropdown.
 *
 * @param id          rider id
 * @param fullName    name
 * @param plateNumber plate
 * @param onDuty      duty switch
 * @param activeJobs  accepted or in-progress jobs
 */
public record RiderOptionResponse(Long id, String fullName, String plateNumber, boolean onDuty, long activeJobs) {

    /**
     * @param rider      a rider
     * @param activeJobs how many jobs they are working on
     * @return the DTO
     */
    public static RiderOptionResponse from(Rider rider, long activeJobs) {
        return new RiderOptionResponse(rider.getId(), rider.getFullName(), rider.getPlateNumber(), rider.isOnDuty(),
                activeJobs);
    }
}
