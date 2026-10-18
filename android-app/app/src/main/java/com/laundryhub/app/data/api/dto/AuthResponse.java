package com.laundryhub.app.data.api.dto;

/** Response of register, login and Google sign-in. Filled by Gson. */
public final class AuthResponse {

    // OOP: Encapsulation — read-only fields set by Gson.
    private String token;
    private String expiresAt;
    private UserProfile user;

    /** For Gson. */
    public AuthResponse() {
    }

    /**
     * @param token     JWT
     * @param expiresAt ISO-8601 instant
     * @param user      the account
     */
    public AuthResponse(String token, String expiresAt, UserProfile user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    /** @return the JWT */
    public String getToken() {
        return token;
    }

    /** @return expiry as ISO-8601 text, e.g. 2026-10-19T02:00:00Z */
    public String getExpiresAt() {
        return expiresAt;
    }

    /** @return the account */
    public UserProfile getUser() {
        return user;
    }
}
