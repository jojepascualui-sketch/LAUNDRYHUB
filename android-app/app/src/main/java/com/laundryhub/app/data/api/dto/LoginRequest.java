package com.laundryhub.app.data.api.dto;

/** Body of {@code POST /api/auth/login}. Gson writes the private fields. */
public final class LoginRequest {

    // OOP: Encapsulation — an immutable request.
    private final String email;
    private final String password;
    private final String role;

    /**
     * @param email    e-mail, or a rider's mobile number
     * @param password password
     * @param role     selected tab: CUSTOMER, RIDER or ADMIN
     */
    public LoginRequest(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    /** @return e-mail or mobile number */
    public String getEmail() {
        return email;
    }

    /** @return selected tab */
    public String getRole() {
        return role;
    }
}
