package com.laundryhub.backend.domain.common;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Argument checks shared by the domain constructors. Every method throws
 * {@link IllegalArgumentException} with a message that names the field.
 */
public final class Check {

    /** Philippine mobile number as typed on sign-up and GCash: 09XXXXXXXXX. */
    public static final Pattern MOBILE_NUMBER = Pattern.compile("^09\\d{9}$");

    /** Digits in a mobile number (column length of mobile and GCash numbers). */
    public static final int MOBILE_LENGTH = 11;

    private static final Pattern EMAIL =Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Check() {
    }

    /**
     * Requires a non-null value.
     *
     * @param value the value to check
     * @param field field name used in the error message
     * @param <T>   value type
     * @return the value
     */
    public static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * Requires non-blank text no longer than {@code maxLength}; returns it trimmed.
     *
     * @param value     the text to check
     * @param field     field name used in the error message
     * @param maxLength maximum length after trimming
     * @return the trimmed text
     */
    public static String text(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    /**
     * Like {@link #text} but allows {@code null} or blank, which become {@code null}.
     *
     * @param value     the text to check
     * @param field     field name used in the error message
     * @param maxLength maximum length after trimming
     * @return the trimmed text, or {@code null}
     */
    public static String optionalText(String value, String field, int maxLength) {
        return value == null || value.isBlank() ? null : text(value, field, maxLength);
    }

    /**
     * Requires a number greater than zero.
     *
     * @param value the number to check
     * @param field field name used in the error message
     * @return the number
     */
    public static BigDecimal positive(BigDecimal value, String field) {
        if (notNull(value, field).signum() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than 0");
        }
        return value;
    }

    /**
     * Requires a number that is zero or more.
     *
     * @param value the number to check
     * @param field field name used in the error message
     * @return the number
     */
    public static BigDecimal nonNegative(BigDecimal value, String field) {
        if (notNull(value, field).signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    /**
     * Requires a mobile number in the 09XXXXXXXXX format.
     *
     * @param value the number to check
     * @param field field name used in the error message
     * @return the number
     */
    public static String mobileNumber(String value, String field) {
        if (value == null || !MOBILE_NUMBER.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must look like 09XXXXXXXXX");
        }
        return value;
    }

    /**
     * Requires a plausible e-mail address; returns it trimmed and lower-cased.
     *
     * @param value     the address to check
     * @param maxLength maximum length
     * @return the normalized address
     */
    public static String email(String value, int maxLength) {
        String trimmed = text(value, "Email", maxLength).toLowerCase();
        if (!EMAIL.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Email is not a valid address");
        }
        return trimmed;
    }
}
