package com.infocusp.trips.domain;

import java.time.Instant;
import java.util.Optional;

/** Validated list criteria; empty optionals mean "no filter". */
public record TripFilter(
        Optional<String> searchTitle,
        Optional<Instant> activeAt,
        Optional<PageCursor> after,
        int pageSize) {}
