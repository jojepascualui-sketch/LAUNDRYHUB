package com.laundryhub.backend.exception;

import org.springframework.http.HttpStatus;

/** Sign-in failed: wrong e-mail or password, or an invalid Google token (401). */
// OOP: Inheritance — a concrete ApiException.
public class UnauthorizedException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message text shown to the user
     */
    public UnauthorizedException(String message) {
        super(message);
    }

    // OOP: Polymorphism — answers with its own status.
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
