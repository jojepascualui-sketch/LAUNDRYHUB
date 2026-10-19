package com.laundryhub.admin.api.dto;

/**
 * Body of {@code POST /api/auth/login} (role ADMIN covers admin and staff).
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record LoginRequest(String email, String password, String role) {
}
