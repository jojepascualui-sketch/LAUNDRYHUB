package com.laundryhub.admin.api.dto;

/**
 * A rider in the "Assign rider" list.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record RiderOption(Long id, String fullName, String plateNumber, boolean onDuty, long activeJobs) {
}
