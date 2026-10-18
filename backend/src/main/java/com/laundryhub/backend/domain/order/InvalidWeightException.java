package com.laundryhub.backend.domain.order;

/** Thrown when a laundry weight is missing, not positive, or below the minimum load. */
public class InvalidWeightException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message what is wrong with the weight
     */
    public InvalidWeightException(String message) {
        super(message);
    }
}
