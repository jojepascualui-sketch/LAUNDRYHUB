package com.laundryhub.app.data.api.dto;

/** Body of {@code POST /api/auth/google}. */
public final class GoogleLoginRequest {

    // OOP: Encapsulation — an immutable request.
    private final String idToken;

    /**
     * @param idToken Google ID token from Credential Manager
     */
    public GoogleLoginRequest(String idToken) {
        this.idToken = idToken;
    }
}
