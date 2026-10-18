package com.laundryhub.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 00a "Continue with Google" (customers only).
 *
 * @param idToken the Google ID token the Android app received
 */
public record GoogleLoginRequest(@NotBlank String idToken) {
}
