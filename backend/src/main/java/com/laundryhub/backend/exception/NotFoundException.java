package com.laundryhub.backend.exception;

import org.springframework.http.HttpStatus;

/** A resource does not exist, or the caller may not see it (404). */
// OOP: Inheritance — a concrete ApiException.
public class NotFoundException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message text shown to the user
     */
    public NotFoundException(String message) {
        super(message);
    }

    // OOP: Polymorphism — answers with its own status.
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
