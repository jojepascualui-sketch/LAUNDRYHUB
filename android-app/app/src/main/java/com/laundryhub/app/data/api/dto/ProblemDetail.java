package com.laundryhub.app.data.api.dto;

import java.util.Collections;
import java.util.Map;

/** The backend's error body (RFC 9457), e.g. {@code {"status":400,"detail":"...","errors":{...}}}. */
public final class ProblemDetail {

    // OOP: Encapsulation — read-only fields set by Gson.
    private int status;
    private String detail;
    private Map<String, String> errors;

    /** For Gson. */
    public ProblemDetail() {
    }

    /** @return HTTP status */
    public int getStatus() {
        return status;
    }

    /** @return message for the user, or {@code null} */
    public String getDetail() {
        return detail;
    }

    /** @return field name → message, never {@code null} */
    public Map<String, String> getErrors() {
        return errors == null ? Collections.emptyMap() : errors;
    }
}
