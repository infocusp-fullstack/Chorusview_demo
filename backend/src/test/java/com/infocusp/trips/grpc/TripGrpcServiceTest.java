package com.infocusp.trips.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.infocusp.trips.TripServer;
import com.infocusp.trips.domain.Stage;
import com.infocusp.trips.domain.Trip;
import com.infocusp.trips.domain.TripNotFoundException;
import com.infocusp.trips.domain.TripPage;
import com.infocusp.trips.service.PageTokenCodec;
import com.infocusp.trips.service.TripService;
import com.infocusp.trips.support.FakeTripRepository;
import com.infocusp.trips.v1.ListTripsRequest;
import com.infocusp.trips.v1.ListTripsResponse;
import com.infocusp.trips.v1.TripServiceGrpc;
import com.infocusp.trips.v1.TripStage;
import com.infocusp.trips.v1.UpdateTripStageRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TripGrpcServiceTest {
    private static final Instant T = Instant.parse("2026-11-01T00:00:00Z");

    private FakeTripRepository repo;
    private TripServer server;
    private ManagedChannel channel;
    private TripServiceGrpc.TripServiceBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        repo = new FakeTripRepository();
        server = new TripServer(0, new TripGrpcService(new TripService(repo, new PageTokenCodec())));
        server.start();
        channel = ManagedChannelBuilder.forAddress("localhost", server.port()).usePlaintext().build();
        stub = TripServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        server.stop();
    }

    @Test
    void listTripsReturnsMappedTrips() {
        repo.pageToReturn = new TripPage(
                List.of(new Trip("a", "Alpha", Stage.NOT_STARTED, false, T, T, T)), Optional.empty());

        ListTripsResponse response = stub.listTrips(ListTripsRequest.newBuilder().setSearchTitle("Al").build());

        assertEquals(1, response.getTripsCount());
        assertEquals("Alpha", response.getTrips(0).getName());
        assertEquals("", response.getNextPageToken());
    }

    @Test
    void updateTripStageReturnsUpdatedTrip() {
        repo.tripToReturn = new Trip("a", "Alpha", Stage.COMPLETED, true, T, T, T);

        com.infocusp.trips.v1.Trip trip = stub.updateTripStage(UpdateTripStageRequest.newBuilder()
                .setTripId("a").setStage(TripStage.COMPLETED).build());

        assertEquals(TripStage.COMPLETED, trip.getStage());
    }

    @Test
    void invalidInputMapsToInvalidArgument() {
        assertCode(Status.Code.INVALID_ARGUMENT, () -> stub.listTrips(ListTripsRequest.newBuilder().setPageSize(-1).build()));
        assertCode(Status.Code.INVALID_ARGUMENT, () -> stub.updateTripStage(
                UpdateTripStageRequest.newBuilder().setTripId("a").build()));
    }

    @Test
    void missingTripMapsToNotFound() {
        repo.failure = new TripNotFoundException("zzz");
        assertCode(Status.Code.NOT_FOUND, () -> stub.updateTripStage(
                UpdateTripStageRequest.newBuilder().setTripId("zzz").setStage(TripStage.COMPLETED).build()));
    }

    @Test
    void unexpectedFailureMapsToInternalWithoutLeakingDetails() {
        repo.failure = new IllegalStateException("db password is hunter2");
        // Capture the server-side log instead of printing it to the console.
        Logger logger = Logger.getLogger(TripGrpcService.class.getName());
        List<LogRecord> logged = new ArrayList<>();
        Handler capture = new Handler() {
            @Override public void publish(LogRecord record) { logged.add(record); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        logger.setUseParentHandlers(false);
        logger.addHandler(capture);
        try {
            StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
                    () -> stub.listTrips(ListTripsRequest.getDefaultInstance()));
            assertEquals(Status.Code.INTERNAL, e.getStatus().getCode());
            assertEquals("internal error", e.getStatus().getDescription());
        } finally {
            logger.removeHandler(capture);
            logger.setUseParentHandlers(true);
        }
        assertEquals(1, logged.size());
        assertEquals(Level.SEVERE, logged.get(0).getLevel());
    }

    @Test
    void blockUntilShutdownReturnsAfterStop() throws Exception {
        Thread waiter = new Thread(() -> {
            try {
                server.blockUntilShutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        waiter.start();
        server.stop();
        waiter.join(5000);
        assertEquals(false, waiter.isAlive());
    }

    private static void assertCode(Status.Code expected, org.junit.jupiter.api.function.Executable call) {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, call);
        assertEquals(expected, e.getStatus().getCode());
    }
}
