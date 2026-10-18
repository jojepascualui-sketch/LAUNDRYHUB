package com.laundryhub.backend.exception;

import org.springframework.http.HttpStatus;

/** The caller is signed in but may not do this (403). */
// OOP: Inheritance — a concrete ApiException.
public class ForbiddenException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message text shown to the user
     */
    public ForbiddenException(String message) {
        super(message);
    }

    // OOP: Polymorphism — answers with its own status.
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
