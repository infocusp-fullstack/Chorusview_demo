package com.infocusp.trips.service;

import com.infocusp.trips.domain.InvalidRequestException;
import com.infocusp.trips.domain.PageCursor;
import com.infocusp.trips.domain.Stage;
import com.infocusp.trips.domain.Trip;
import com.infocusp.trips.domain.TripFilter;
import com.infocusp.trips.domain.TripPage;
import com.infocusp.trips.repository.TripRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/** Validates input and orchestrates the repository. */
public final class TripService {
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private final TripRepository repository;
    private final PageTokenCodec tokens;

    public TripService(TripRepository repository, PageTokenCodec tokens) {
        this.repository = repository;
        this.tokens = tokens;
    }

    public TripListing listTrips(String searchTitle, String activeAtTime, int pageSize, String pageToken) {
        TripFilter filter = new TripFilter(
                Optional.of(searchTitle.strip()).filter(s -> !s.isEmpty()),
                Optional.of(activeAtTime).filter(s -> !s.isEmpty()).map(TripService::parseInstant),
                Optional.of(pageToken).filter(s -> !s.isEmpty()).map(tokens::decode),
                resolvePageSize(pageSize));
        TripPage page = repository.find(filter);
        String next = page.next().map(tokens::encode).orElse("");
        return new TripListing(page.trips(), next);
    }

    public Trip updateStage(String tripId, Stage stage) {
        if (tripId.isBlank()) {
            throw new InvalidRequestException("trip_id is required");
        }
        return repository.updateStage(tripId, stage);
    }

    private static int resolvePageSize(int requested) {
        if (requested < 0) {
            throw new InvalidRequestException("page_size must not be negative");
        }
        if (requested == 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }

    private static Instant parseInstant(String raw) {
        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("active_at_time must be an ISO-8601 timestamp");
        }
    }
}
