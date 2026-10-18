package com.laundryhub.backend.exception;

import org.springframework.http.HttpStatus;

/** The request clashes with the current state, e.g. a booking another rider already took (409). */
// OOP: Inheritance — a concrete ApiException.
public class ConflictException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message text shown to the user
     */
    public ConflictException(String message) {
        super(message);
    }

    // OOP: Polymorphism — answers with its own status.
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
