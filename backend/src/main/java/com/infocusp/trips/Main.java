package com.infocusp.trips;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.infocusp.trips.config.AppConfig;
import com.infocusp.trips.grpc.TripGrpcService;
import com.infocusp.trips.repository.SpannerTripRepository;
import com.infocusp.trips.service.PageTokenCodec;
import com.infocusp.trips.service.TripService;

/** Wires dependencies and runs the server until the JVM is stopped. */
public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromEnv(System.getenv());
        // SPANNER_EMULATOR_HOST, when set, is picked up by the client library.
        try (Spanner spanner = SpannerOptions.newBuilder().setProjectId(config.project()).build().getService()) {
            DatabaseClient client = spanner.getDatabaseClient(
                    DatabaseId.of(config.project(), config.instance(), config.database()));
            TripService service = new TripService(new SpannerTripRepository(client), new PageTokenCodec());
            TripServer server = new TripServer(config.grpcPort(), new TripGrpcService(service));
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            server.blockUntilShutdown();
        }
    }
}
