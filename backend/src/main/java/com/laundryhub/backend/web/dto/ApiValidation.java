package com.laundryhub.backend.web.dto;

/** Patterns shared by request DTOs. */
public final class ApiValidation {

    /** Philippine mobile number 09XXXXXXXXX (sign-up, GCash). */
    public static final String MOBILE_REGEX = "^09\\d{9}$";

    /** Message for a malformed mobile number. */
    public static final String MOBILE_MESSAGE = "must look like 09XXXXXXXXX";

    /** Shortest password accepted ("At least 8 characters"). */
    public static final int MIN_PASSWORD_LENGTH = 8;

    /** Longest password accepted (BCrypt reads at most 72 bytes). */
    public static final int MAX_PASSWORD_LENGTH = 72;

    private ApiValidation() {
    }
}
