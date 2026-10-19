package com.laundryhub.admin.model;

import com.laundryhub.admin.api.dto.AuthResponse;

/** The signed-in admin or staff member. Kept in memory only (never written to disk). */
public final class Session {

    private static final String DEFAULT_POSITION = "Owner";
    private static final String STAFF_POSITION = "Staff";
    private static final int AVATAR_LETTERS = 2;

    // OOP: Encapsulation — set once at sign-in.
    private final String token;
    private final String fullName;
    private final String initials;
    private final String position;

    /**
     * @param token    bearer token
     * @param fullName "Admin"
     * @param initials "AD"
     * @param position "Owner"
     * @throws IllegalArgumentException if the token or name is blank
     */
    public Session(String token, String fullName, String initials, String position) {
        if (token == null || token.isBlank() || fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Token and name are required");
        }
        this.token = token;
        this.fullName = fullName;
        this.initials = avatarText(fullName, initials);
        this.position = position == null || position.isBlank() ? DEFAULT_POSITION : position;
    }

    /**
     * @param fullName name
     * @param initials server initials ("A" for "Admin")
     * @return two letters for the avatar, as in the design ("AD")
     */
    static String avatarText(String fullName, String initials) {
        String name = fullName.trim();
        if ((initials == null || initials.length() < AVATAR_LETTERS) && !name.contains(" ")
                && name.length() >= AVATAR_LETTERS) {
            return name.substring(0, AVATAR_LETTERS).toUpperCase(java.util.Locale.ROOT);
        }
        return initials == null ? "" : initials;
    }

    /**
     * @param response sign-in result
     * @return the session
     */
    public static Session from(AuthResponse response) {
        String position = response.user().position();
        if ((position == null || position.isBlank()) && Role.STAFF.name().equals(response.user().role())) {
            position = STAFF_POSITION;
        }
        return new Session(response.token(), response.user().fullName(), response.user().initials(), position);
    }

    /** @return bearer token */
    public String token() {
        return token;
    }

    /** @return name in the sidebar */
    public String fullName() {
        return fullName;
    }

    /** @return avatar initials */
    public String initials() {
        return initials;
    }

    /** @return "Owner" / "Counter staff" */
    public String position() {
        return position;
    }
}
