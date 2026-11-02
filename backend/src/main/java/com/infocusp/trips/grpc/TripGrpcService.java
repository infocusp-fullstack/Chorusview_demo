package com.infocusp.trips.grpc;

import com.infocusp.trips.domain.InvalidRequestException;
import com.infocusp.trips.domain.TripNotFoundException;
import com.infocusp.trips.service.TripListing;
import com.infocusp.trips.service.TripService;
import com.infocusp.trips.v1.ListTripsRequest;
import com.infocusp.trips.v1.ListTripsResponse;
import com.infocusp.trips.v1.Trip;
import com.infocusp.trips.v1.TripServiceGrpc;
import com.infocusp.trips.v1.UpdateTripStageRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.function.Supplier;

/** gRPC transport adapter: delegates to TripService and maps errors to status codes. */
public final class TripGrpcService extends TripServiceGrpc.TripServiceImplBase {
    private static final System.Logger LOG = System.getLogger(TripGrpcService.class.getName());

    private final TripService service;

    public TripGrpcService(TripService service) {
        this.service = service;
    }

    @Override
    public void listTrips(ListTripsRequest request, StreamObserver<ListTripsResponse> observer) {
        respond(observer, () -> {
            TripListing listing = service.listTrips(
                    request.getSearchTitle(), request.getActiveAtTime(), request.getPageSize(), request.getPageToken());
            return ListTripsResponse.newBuilder()
                    .addAllTrips(listing.trips().stream().map(TripMapper::toProto).toList())
                    .setNextPageToken(listing.nextPageToken())
                    .build();
        });
    }

    @Override
    public void updateTripStage(UpdateTripStageRequest request, StreamObserver<Trip> observer) {
        respond(observer, () -> TripMapper.toProto(
                service.updateStage(request.getTripId(), TripMapper.toDomain(request.getStage()))));
    }

    private static <T> void respond(StreamObserver<T> observer, Supplier<T> action) {
        T result;
        try {
            result = action.get();
        } catch (InvalidRequestException e) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
            return;
        } catch (TripNotFoundException e) {
            observer.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
            return;
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR, "request failed", e);
            observer.onError(Status.INTERNAL.withDescription("internal error").asRuntimeException());
            return;
        }
        observer.onNext(result);
        observer.onCompleted();
    }
}
