package com.laundryhub.backend.web.dto;

import java.time.Instant;

/**
 * Result of register, login and Google sign-in.
 *
 * @param token     JWT, sent as {@code Authorization: Bearer <token>}
 * @param tokenType always "Bearer"
 * @param expiresAt when the token stops working
 * @param user      the account (its {@code role} decides which app screen comes next)
 */
public record AuthResponse(String token, String tokenType, Instant expiresAt, UserProfileResponse user) {

    /** Value of {@link #tokenType}. */
    public static final String BEARER = "Bearer";
}
