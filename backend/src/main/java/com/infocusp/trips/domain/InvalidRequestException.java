package com.infocusp.trips.domain;

/** The caller supplied invalid input. */
public final class InvalidRequestException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(message);
    }
}
