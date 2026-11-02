package com.infocusp.trips.domain;

import java.time.Instant;

/** Keyset position: the last row of the previous page. */
public record PageCursor(Instant createdAt, String tripId) {}
