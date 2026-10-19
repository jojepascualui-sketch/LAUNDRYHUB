package com.laundryhub.admin.api.dto;

/**
 * Body of {@code PUT /api/admin/users/{id}} (blank password = unchanged).
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record UserUpdate(String fullName, String mobileNumber, String status, String plateNumber, String position, String password) {
}
