package com.infocusp.trips.domain;

import java.util.List;
import java.util.Optional;

/** One page of trips plus the cursor of the next page, if any. */
public record TripPage(List<Trip> trips, Optional<PageCursor> next) {}
