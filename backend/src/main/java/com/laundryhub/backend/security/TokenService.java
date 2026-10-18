package com.laundryhub.backend.security;

import java.time.Instant;

import com.laundryhub.backend.domain.user.User;

/** Issues the bearer tokens returned by sign-in. */
// OOP: Abstraction — AuthService does not know tokens are JWTs signed with HS256.
public interface TokenService {

    /** JWT claim holding the account role, e.g. {@code "RIDER"}. */
    String ROLE_CLAIM = "role";

    /** JWT claim holding the full name. */
    String NAME_CLAIM = "name";

    /**
     * Creates a token for an account. The subject is the user id.
     *
     * @param user a saved account
     * @return the token and when it expires
     */
    IssuedToken issue(User user);

    /**
     * A signed token.
     *
     * @param value     the token text sent as {@code Authorization: Bearer <value>}
     * @param expiresAt when it stops being accepted
     */
    record IssuedToken(String value, Instant expiresAt) {
    }
}
