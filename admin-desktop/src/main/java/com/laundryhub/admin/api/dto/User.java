package com.laundryhub.admin.api.dto;

/**
 * A customer, rider, admin or staff account (A4 rows).
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record User(Long id, String role, String fullName, String email, String mobileNumber, String initials, String status, Long orderCount, String plateNumber, Boolean onDuty, String position) {
}
