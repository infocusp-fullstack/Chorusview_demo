package com.infocusp.trips.domain;

import java.time.Instant;

/** A trip; all instants are UTC. */
public record Trip(
        String id,
        String name,
        Stage stage,
        boolean active,
        Instant startTime,
        Instant endTime,
        Instant createdAt) {

    public Trip withStage(Stage newStage) {
        return new Trip(id, name, newStage, active, startTime, endTime, createdAt);
    }
}
