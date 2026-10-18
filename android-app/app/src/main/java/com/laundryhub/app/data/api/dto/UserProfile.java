package com.laundryhub.app.data.api.dto;

/** The {@code user} part of the sign-in response. Filled by Gson. */
public final class UserProfile {

    // OOP: Encapsulation — read-only fields set by Gson.
    private Long id;
    private String role;
    private String fullName;
    private String email;
    private String initials;

    /** For Gson. */
    public UserProfile() {
    }

    /**
     * @param id       user id
     * @param role     role text
     * @param fullName full name
     */
    public UserProfile(Long id, String role, String fullName) {
        this.id = id;
        this.role = role;
        this.fullName = fullName;
    }

    /** @return user id */
    public Long getId() {
        return id;
    }

    /** @return role text, e.g. "RIDER" */
    public String getRole() {
        return role;
    }

    /** @return full name */
    public String getFullName() {
        return fullName;
    }

    /** @return e-mail */
    public String getEmail() {
        return email;
    }

    /** @return avatar initials */
    public String getInitials() {
        return initials;
    }
}
