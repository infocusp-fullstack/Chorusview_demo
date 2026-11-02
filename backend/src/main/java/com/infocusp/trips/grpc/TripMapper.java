package com.infocusp.trips.grpc;

import com.google.protobuf.Timestamp;
import com.infocusp.trips.domain.InvalidRequestException;
import com.infocusp.trips.domain.Stage;
import com.infocusp.trips.domain.Trip;
import com.infocusp.trips.v1.TripStage;
import java.time.Instant;

/** Maps between domain objects and protobuf messages. */
final class TripMapper {
    private TripMapper() {}

    static com.infocusp.trips.v1.Trip toProto(Trip trip) {
        return com.infocusp.trips.v1.Trip.newBuilder()
                .setTripId(trip.id())
                .setName(trip.name())
                .setStage(TripStage.valueOf(trip.stage().name()))
                .setIsActive(trip.active())
                .setStartTime(toProto(trip.startTime()))
                .setEndTime(toProto(trip.endTime()))
                .setCreatedAt(toProto(trip.createdAt()))
                .build();
    }

    static Stage toDomain(TripStage stage) {
        return switch (stage) {
            case NOT_STARTED -> Stage.NOT_STARTED;
            case IN_TRANSIT -> Stage.IN_TRANSIT;
            case COMPLETED -> Stage.COMPLETED;
            case TRIP_STAGE_UNSPECIFIED, UNRECOGNIZED -> throw new InvalidRequestException("a valid stage is required");
        };
    }

    private static Timestamp toProto(Instant i) {
        return Timestamp.newBuilder().setSeconds(i.getEpochSecond()).setNanos(i.getNano()).build();
    }
}
