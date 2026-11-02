package com.infocusp.trips.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void usesDefaultsWhenEnvIsEmpty() {
        assertEquals(new AppConfig(9090, "trips-project", "trips-instance", "trips-db"), AppConfig.fromEnv(Map.of()));
    }

    @Test
    void readsOverrides() {
        AppConfig config = AppConfig.fromEnv(Map.of(
                "GRPC_PORT", "7000", "SPANNER_PROJECT", "p", "SPANNER_INSTANCE", "i", "SPANNER_DATABASE", "d"));
        assertEquals(new AppConfig(7000, "p", "i", "d"), config);
    }

    @Test
    void rejectsNonNumericPort() {
        assertThrows(IllegalArgumentException.class, () -> AppConfig.fromEnv(Map.of("GRPC_PORT", "abc")));
    }
}
