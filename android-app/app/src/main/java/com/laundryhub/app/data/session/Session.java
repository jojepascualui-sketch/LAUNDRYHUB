package com.laundryhub.app.data.session;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.api.dto.AuthResponse;
import com.laundryhub.app.model.Role;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/** A signed-in account: JWT, expiry, role and name. Immutable. */
public final class Session {

    // OOP: Encapsulation — validated once, never changed.
    private final String token;
    private final long expiresAtMillis;
    private final Role role;
    private final String fullName;

    /**
     * @param token           JWT
     * @param expiresAtMillis expiry, epoch milliseconds
     * @param role            account role
     * @param fullName        account name
     * @throws IllegalArgumentException if a value is missing
     */
    public Session(String token, long expiresAtMillis, Role role, String fullName) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        this.token = token;
        this.expiresAtMillis = expiresAtMillis;
        this.role = role;
        this.fullName = fullName == null ? "" : fullName;
    }

    /**
     * @param response sign-in response
     * @return the session
     * @throws IllegalArgumentException if the response is incomplete
     */
    @NonNull
    public static Session from(@NonNull AuthResponse response) {
        if (response.getUser() == null || response.getUser().getRole() == null || response.getExpiresAt() == null) {
            throw new IllegalArgumentException("Incomplete sign-in response");
        }
        try {
            return new Session(response.getToken(), Instant.parse(response.getExpiresAt()).toEpochMilli(),
                    Role.fromApi(response.getUser().getRole()), response.getUser().getFullName());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Bad expiry: " + response.getExpiresAt(), e);
        }
    }

    /**
     * @param nowMillis current time
     * @return whether the token has expired
     */
    public boolean isExpired(long nowMillis) {
        return nowMillis >= expiresAtMillis;
    }

    /** @return the JWT */
    @NonNull
    public String getToken() {
        return token;
    }

    /** @return expiry, epoch milliseconds */
    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    /** @return the role */
    @NonNull
    public Role getRole() {
        return role;
    }

    /** @return the account name */
    @NonNull
    public String getFullName() {
        return fullName;
    }
}
