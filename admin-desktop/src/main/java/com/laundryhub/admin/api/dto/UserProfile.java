package com.laundryhub.admin.api.dto;

/**
 * The signed-in admin or staff member (sidebar profile).
 *
 * <p>Immutable; filled by Jackson from the backend JSON.
 */
public record UserProfile(Long id, String role, String fullName, String email, String initials, String position) {
}
