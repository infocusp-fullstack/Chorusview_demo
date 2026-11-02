package com.infocusp.trips.repository;

import com.infocusp.trips.domain.Stage;
import com.infocusp.trips.domain.Trip;
import com.infocusp.trips.domain.TripFilter;
import com.infocusp.trips.domain.TripPage;

/** Persistence port for trips. */
public interface TripRepository {

    TripPage find(TripFilter filter);

    /** Sets the stage and returns the updated trip; throws TripNotFoundException if absent. */
    Trip updateStage(String tripId, Stage stage);
}
