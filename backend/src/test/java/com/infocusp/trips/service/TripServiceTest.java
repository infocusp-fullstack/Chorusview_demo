package com.infocusp.trips.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infocusp.trips.domain.InvalidRequestException;
import com.infocusp.trips.domain.PageCursor;
import com.infocusp.trips.domain.Stage;
import com.infocusp.trips.domain.Trip;
import com.infocusp.trips.domain.TripPage;
import com.infocusp.trips.support.FakeTripRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TripServiceTest {
    private final PageTokenCodec codec = new PageTokenCodec();
    private FakeTripRepository repo;
    private TripService service;

    @BeforeEach
    void setUp() {
        repo = new FakeTripRepository();
        service = new TripService(repo, codec);
    }

    @Test
    void emptyInputsMeanNoFiltersAndDefaultPageSize() {
        TripListing listing = service.listTrips("", "", 0, "");
        assertTrue(repo.lastFilter.searchTitle().isEmpty());
        assertTrue(repo.lastFilter.activeAt().isEmpty());
        assertTrue(repo.lastFilter.after().isEmpty());
        assertEquals(TripService.DEFAULT_PAGE_SIZE, repo.lastFilter.pageSize());
        assertEquals("", listing.nextPageToken());
    }

    @Test
    void blankTitleIsIgnoredAndValuesAreStripped() {
        service.listTrips("   ", "", 5, "");
        assertTrue(repo.lastFilter.searchTitle().isEmpty());
        assertEquals(5, repo.lastFilter.pageSize());
        service.listTrips("  Paris ", "", 5, "");
        assertEquals(Optional.of("Paris"), repo.lastFilter.searchTitle());
    }

    @Test
    void parsesActiveAtTimeWithOffsetToUtc() {
        service.listTrips("", "2026-11-01T10:00:00+05:30", 1, "");
        assertEquals(Optional.of(Instant.parse("2026-11-01T04:30:00Z")), repo.lastFilter.activeAt());
    }

    @Test
    void clampsPageSizeToMax() {
        service.listTrips("", "", 5000, "");
        assertEquals(TripService.MAX_PAGE_SIZE, repo.lastFilter.pageSize());
    }

    @Test
    void decodesIncomingTokenAndEncodesNextCursor() {
        PageCursor in = new PageCursor(Instant.parse("2026-01-01T00:00:00Z"), "a");
        PageCursor out = new PageCursor(Instant.parse("2026-01-02T00:00:00Z"), "b");
        repo.pageToReturn = new TripPage(List.of(), Optional.of(out));

        TripListing listing = service.listTrips("", "", 1, codec.encode(in));

        assertEquals(Optional.of(in), repo.lastFilter.after());
        assertEquals(out, codec.decode(listing.nextPageToken()));
    }

    @Test
    void rejectsNegativePageSize() {
        assertThrows(InvalidRequestException.class, () -> service.listTrips("", "", -1, ""));
    }

    @Test
    void rejectsBadActiveAtTime() {
        assertThrows(InvalidRequestException.class, () -> service.listTrips("", "tomorrow", 1, ""));
    }

    @Test
    void rejectsBadPageToken() {
        assertThrows(InvalidRequestException.class, () -> service.listTrips("", "", 1, "!!!"));
    }

    @Test
    void updateStageDelegatesToRepository() {
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        repo.tripToReturn = new Trip("id", "n", Stage.COMPLETED, true, t, t, t);
        assertSame(repo.tripToReturn, service.updateStage("id", Stage.COMPLETED));
        assertEquals("id", repo.lastTripId);
        assertEquals(Stage.COMPLETED, repo.lastStage);
    }

    @Test
    void updateStageRejectsBlankId() {
        assertThrows(InvalidRequestException.class, () -> service.updateStage(" ", Stage.COMPLETED));
    }
}
