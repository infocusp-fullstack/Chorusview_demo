package com.infocusp.trips;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** Lifecycle wrapper around the gRPC server. */
public final class TripServer {
    private static final long STOP_TIMEOUT_SECONDS = 5;

    private final Server server;

    public TripServer(int port, BindableService service) {
        this.server = ServerBuilder.forPort(port).addService(service).build();
    }

    public void start() throws IOException {
        server.start();
    }

    /** Actual bound port (useful when started with port 0). */
    public int port() {
        return server.getPort();
    }

    public void stop() throws InterruptedException {
        server.shutdown().awaitTermination(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public void blockUntilShutdown() throws InterruptedException {
        server.awaitTermination();
    }
}
