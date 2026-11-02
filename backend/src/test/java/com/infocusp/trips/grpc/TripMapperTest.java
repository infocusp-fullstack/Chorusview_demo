package com.infocusp.trips.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.infocusp.trips.domain.InvalidRequestException;
import com.infocusp.trips.domain.Stage;
import com.infocusp.trips.domain.Trip;
import com.infocusp.trips.v1.TripStage;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TripMapperTest {

    @Test
    void mapsTripToProto() {
        Instant start = Instant.parse("2026-11-01T10:00:00.5Z");
        Instant end = Instant.parse("2026-11-02T10:00:00Z");
        Instant created = Instant.parse("2026-10-01T00:00:00Z");

        com.infocusp.trips.v1.Trip proto =
                TripMapper.toProto(new Trip("id-1", "Paris", Stage.IN_TRANSIT, true, start, end, created));

        assertEquals("id-1", proto.getTripId());
        assertEquals("Paris", proto.getName());
        assertEquals(TripStage.IN_TRANSIT, proto.getStage());
        assertEquals(true, proto.getIsActive());
        assertEquals(start.getEpochSecond(), proto.getStartTime().getSeconds());
        assertEquals(500_000_000, proto.getStartTime().getNanos());
        assertEquals(end.getEpochSecond(), proto.getEndTime().getSeconds());
        assertEquals(created.getEpochSecond(), proto.getCreatedAt().getSeconds());
    }

    @Test
    void mapsEveryConcreteStage() {
        assertEquals(Stage.NOT_STARTED, TripMapper.toDomain(TripStage.NOT_STARTED));
        assertEquals(Stage.IN_TRANSIT, TripMapper.toDomain(TripStage.IN_TRANSIT));
        assertEquals(Stage.COMPLETED, TripMapper.toDomain(TripStage.COMPLETED));
    }

    @Test
    void rejectsUnspecifiedAndUnrecognizedStages() {
        assertThrows(InvalidRequestException.class, () -> TripMapper.toDomain(TripStage.TRIP_STAGE_UNSPECIFIED));
        assertThrows(InvalidRequestException.class, () -> TripMapper.toDomain(TripStage.UNRECOGNIZED));
    }
}
