package com.infocusp.trips.domain;

/** No trip exists for the given id. */
public final class TripNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TripNotFoundException(String tripId) {
        super("trip not found: " + tripId);
    }
}
