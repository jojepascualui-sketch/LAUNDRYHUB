package com.laundryhub.backend.exception;

import org.springframework.http.HttpStatus;

/** A feature that needs server configuration is not set up, e.g. Google sign-in or the shop location (503). */
// OOP: Inheritance — a concrete ApiException.
public class ServiceUnavailableException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message text shown to the user
     */
    public ServiceUnavailableException(String message) {
        super(message);
    }

    // OOP: Polymorphism — answers with its own status.
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}
