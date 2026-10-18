package com.laundryhub.app.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A failed API call: the server's message and per-field errors, or a network failure
 * ({@link #isNetworkError()}).
 */
public class ApiException extends Exception {

    /** Status used when the server could not be reached. */
    public static final int NO_RESPONSE = 0;

    private static final long serialVersionUID = 1L;

    // OOP: Encapsulation — immutable error details.
    private final int status;
    private final Map<String, String> fieldErrors;

    /**
     * @param status      HTTP status, or {@link #NO_RESPONSE}
     * @param message     message for the user, may be {@code null}
     * @param fieldErrors field name → message
     */
    public ApiException(int status, @Nullable String message, @Nullable Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(fieldErrors));
    }

    /**
     * @param cause the I/O failure
     * @return a network error
     */
    @NonNull
    public static ApiException network(@NonNull Throwable cause) {
        ApiException e = new ApiException(NO_RESPONSE, null, null);
        e.initCause(cause);
        return e;
    }

    /** @return HTTP status, or {@link #NO_RESPONSE} */
    public int getStatus() {
        return status;
    }

    /** @return whether the server could not be reached */
    public boolean isNetworkError() {
        return status == NO_RESPONSE;
    }

    /** @return field name → message (read-only) */
    @NonNull
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
