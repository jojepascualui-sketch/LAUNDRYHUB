package com.laundryhub.admin.api.dto;

/**
 * Sign-in result: bearer token and profile.
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record AuthResponse(String token, String expiresAt, UserProfile user) {
}
