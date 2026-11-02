package com.infocusp.trips.config;

import java.util.Map;

/** Runtime settings read from environment variables. */
public record AppConfig(int grpcPort, String project, String instance, String database) {

    public static AppConfig fromEnv(Map<String, String> env) {
        return new AppConfig(
                parsePort(env.getOrDefault("GRPC_PORT", "9090")),
                env.getOrDefault("SPANNER_PROJECT", "trips-project"),
                env.getOrDefault("SPANNER_INSTANCE", "trips-instance"),
                env.getOrDefault("SPANNER_DATABASE", "trips-db"));
    }

    private static int parsePort(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("GRPC_PORT is not a number: " + raw, e);
        }
    }
}
