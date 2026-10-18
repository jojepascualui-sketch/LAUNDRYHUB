package com.laundryhub.backend.web.dto;

import com.laundryhub.backend.domain.user.Role;

import jakarta.validation.constraints.NotBlank;

/**
 * 00a / 00c "Sign in".
 *
 * @param email    Email ("Staff email" on the Admin tab); on the Rider tab also a mobile number
 *                 09XXXXXXXXX ("Rider email or mobile")
 * @param password Password
 * @param role     the selected tab: CUSTOMER, RIDER or ADMIN (ADMIN also lets STAFF in);
 *                 {@code null} accepts any role
 */
public record LoginRequest(@NotBlank String email, @NotBlank String password, Role role) {
}
