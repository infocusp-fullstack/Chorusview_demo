package com.infocusp.trips.support;

import com.infocusp.trips.domain.Stage;
import com.infocusp.trips.domain.Trip;
import com.infocusp.trips.domain.TripFilter;
import com.infocusp.trips.domain.TripPage;
import com.infocusp.trips.repository.TripRepository;
import java.util.List;
import java.util.Optional;

/** Scriptable in-memory repository that records the last call. */
public final class FakeTripRepository implements TripRepository {
    public TripFilter lastFilter;
    public String lastTripId;
    public Stage lastStage;
    public TripPage pageToReturn = new TripPage(List.of(), Optional.empty());
    public Trip tripToReturn;
    public RuntimeException failure;

    @Override
    public TripPage find(TripFilter filter) {
        lastFilter = filter;
        if (failure != null) {
            throw failure;
        }
        return pageToReturn;
    }

    @Override
    public Trip updateStage(String tripId, Stage stage) {
        lastTripId = tripId;
        lastStage = stage;
        if (failure != null) {
            throw failure;
        }
        return tripToReturn;
    }
}
