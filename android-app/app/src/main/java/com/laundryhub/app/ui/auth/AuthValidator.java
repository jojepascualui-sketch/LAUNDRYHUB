package com.laundryhub.app.ui.auth;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

/** Field rules shared by sign-in and sign-up. The server checks them again. */
public final class AuthValidator {

    /** Shortest password accepted ("At least 8 characters"). */
    public static final int MIN_PASSWORD_LENGTH = 8;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern MOBILE = Pattern.compile("^09\\d{9}$");
    private static final Pattern MOBILE_SEPARATORS = Pattern.compile("[\\s-]");

    private AuthValidator() {
    }

    /**
     * @param text trimmed input
     * @return whether it looks like an e-mail address
     */
    public static boolean isEmail(@NonNull String text) {
        return EMAIL.matcher(text).matches();
    }

    /**
     * @param text input such as "0917 123 4567"
     * @return the number without spaces or dashes, e.g. "09171234567"
     */
    @NonNull
    public static String normalizeMobile(@NonNull String text) {
        return MOBILE_SEPARATORS.matcher(text).replaceAll("");
    }

    /**
     * @param text input, spaces allowed
     * @return whether it is a mobile number 09XXXXXXXXX
     */
    public static boolean isMobile(@NonNull String text) {
        return MOBILE.matcher(normalizeMobile(text)).matches();
    }

    /**
     * @param password the password
     * @return whether it is long enough
     */
    public static boolean isLongEnough(@NonNull String password) {
        return password.length() >= MIN_PASSWORD_LENGTH;
    }
}
