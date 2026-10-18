package com.laundryhub.backend.security;

/** Checks a Google ID token from "Continue with Google" (00a, customers only). */
// OOP: Abstraction — AuthService does not know how Google tokens are verified; tests swap in a fake.
public interface GoogleTokenVerifier {

    /**
     * Verifies the token's signature, issuer, audience and expiry.
     *
     * @param idToken the ID token the Android app received from Google
     * @return the verified identity
     * @throws com.laundryhub.backend.exception.UnauthorizedException       if the token is not valid
     * @throws com.laundryhub.backend.exception.ServiceUnavailableException if Google sign-in is not configured
     */
    GoogleIdentity verify(String idToken);

    /**
     * The verified Google account.
     *
     * @param email         e-mail address
     * @param fullName      display name, may be {@code null}
     * @param emailVerified whether Google verified the address
     */
    record GoogleIdentity(String email, String fullName, boolean emailVerified) {
    }
}
