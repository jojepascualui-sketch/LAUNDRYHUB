package com.laundryhub.admin.api.dto;

/**
 * Body of {@code POST /api/admin/users}.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record UserCreate(String role, String fullName, String email, String mobileNumber, String password, String plateNumber, String position) {
}
