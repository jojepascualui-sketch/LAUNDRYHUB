package com.laundryhub.admin.api;

import java.util.Map;

/** A failed API call: HTTP status and the server's message (problem details), or a network error. */
public class ApiException extends RuntimeException {

    /** Status used when the server could not be reached. */
    public static final int NO_RESPONSE = 0;
    /** Signed out or token expired. */
    public static final int UNAUTHORIZED = 401;

    private static final long serialVersionUID = 1L;

    // OOP: Encapsulation — immutable error details.
    private final int status;
    private final transient Map<String, String> fieldErrors;

    /**
     * @param status      HTTP status or {@link #NO_RESPONSE}
     * @param message     message for the admin
     * @param fieldErrors field name → message (may be empty)
     */
    public ApiException(int status, String message, Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }

    /** @return HTTP status, or {@link #NO_RESPONSE} */
    public int getStatus() {
        return status;
    }

    /** @return whether the sign-in expired */
    public boolean isUnauthorized() {
        return status == UNAUTHORIZED;
    }

    /** @return field name → message */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
