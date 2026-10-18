package com.laundryhub.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * An error the API reports to the client with a specific HTTP status. The global exception
 * handler turns it into a problem-details JSON body.
 */
// OOP: Abstraction — the handler only knows "an API error with a status".
// OOP: Inheritance — NotFoundException, ConflictException, ... extend this class.
public abstract class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message text shown to the user
     */
    protected ApiException(String message) {
        super(message);
    }

    /** @return the HTTP status to answer with */
    // OOP: Polymorphism — each subclass maps to its own status code.
    public abstract HttpStatus getStatus();
}
