package com.infocusp.trips.service;

import com.infocusp.trips.domain.Trip;
import java.util.List;

/** Result of a list call; nextPageToken is empty on the last page. */
public record TripListing(List<Trip> trips, String nextPageToken) {}
