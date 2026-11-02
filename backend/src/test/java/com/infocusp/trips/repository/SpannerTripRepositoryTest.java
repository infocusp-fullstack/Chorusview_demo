package com.infocusp.trips.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.gson.Gson;
import com.infocusp.trips.domain.PageCursor;
import com.infocusp.trips.domain.Stage;
import com.infocusp.trips.domain.Trip;
import com.infocusp.trips.domain.TripFilter;
import com.infocusp.trips.domain.TripNotFoundException;
import com.infocusp.trips.domain.TripPage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Runs against the real Cloud Spanner emulator using the production schema. */
@Testcontainers(disabledWithoutDocker = true)
class SpannerTripRepositoryTest {
    private static final String PROJECT = "test-project";
    private static final String INSTANCE = "test-instance";
    private static final String DATABASE = "test-db";
    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Container
    static final GenericContainer<?> EMULATOR =
            new GenericContainer<>("gcr.io/cloud-spanner-emulator/emulator:latest").withExposedPorts(9010, 9020);

    private static Spanner spanner;
    private static DatabaseClient client;
    private SpannerTripRepository repo;

    @BeforeAll
    static void createDatabase() throws Exception {
        String rest = "http://localhost:" + EMULATOR.getMappedPort(9020) + "/v1/projects/" + PROJECT + "/instances";
        post(rest, Map.of("instanceId", INSTANCE, "instance", Map.of(
                "config", "projects/" + PROJECT + "/instanceConfigs/emulator-config",
                "displayName", "test", "nodeCount", 1)));
        post(rest + "/" + INSTANCE + "/databases", Map.of(
                "createStatement", "CREATE DATABASE `" + DATABASE + "`",
                "extraStatements", schemaStatements()));
        spanner = SpannerOptions.newBuilder()
                .setProjectId(PROJECT)
                .setEmulatorHost("localhost:" + EMULATOR.getMappedPort(9010))
                .build().getService();
        client = spanner.getDatabaseClient(DatabaseId.of(PROJECT, INSTANCE, DATABASE));
    }

    @AfterAll
    static void closeSpanner() {
        spanner.close();
    }

    @BeforeEach
    void resetData() {
        repo = new SpannerTripRepository(client);
        client.write(List.of(Mutation.delete("Trips", com.google.cloud.spanner.KeySet.all())));
        List<Mutation> rows = new ArrayList<>();
        rows.add(row("t1", "Paris Express", Stage.NOT_STARTED, 0, 0, 10));
        rows.add(row("t2", "paris local", Stage.IN_TRANSIT, 1, 5, 15));
        rows.add(row("t3", "Berlin 100% Run", Stage.COMPLETED, 2, 20, 30));
        rows.add(row("t4", "Berlin_Night", Stage.NOT_STARTED, 3, 25, 35));
        rows.add(row("t5", "Rome", Stage.IN_TRANSIT, 4, 40, 50));
        client.write(rows);
    }

    @Test
    void listsAllTripsInCreationOrder() {
        TripPage page = repo.find(filter(Optional.empty(), Optional.empty(), Optional.empty(), 10));
        assertEquals(List.of("t1", "t2", "t3", "t4", "t5"), ids(page));
        assertTrue(page.next().isEmpty());
        assertEquals(Stage.NOT_STARTED, page.trips().get(0).stage());
        assertEquals(BASE, page.trips().get(0).startTime());
    }

    @Test
    void pagesThroughAllRowsWithoutGapsOrDuplicates() {
        List<String> seen = new ArrayList<>();
        Optional<PageCursor> cursor = Optional.empty();
        int pages = 0;
        do {
            TripPage page = repo.find(filter(Optional.empty(), Optional.empty(), cursor, 2));
            seen.addAll(ids(page));
            cursor = page.next();
            pages++;
        } while (cursor.isPresent());
        assertEquals(List.of("t1", "t2", "t3", "t4", "t5"), seen);
        assertEquals(3, pages);
    }

    @Test
    void searchesTitleCaseInsensitively() {
        TripPage page = repo.find(filter(Optional.of("PARIS"), Optional.empty(), Optional.empty(), 10));
        assertEquals(List.of("t1", "t2"), ids(page));
    }

    @Test
    void treatsLikeWildcardsAsLiterals() {
        assertEquals(List.of("t3"), ids(repo.find(filter(Optional.of("100%"), Optional.empty(), Optional.empty(), 10))));
        assertEquals(List.of("t4"), ids(repo.find(filter(Optional.of("n_n"), Optional.empty(), Optional.empty(), 10))));
        assertEquals(List.of("t3"), ids(repo.find(filter(Optional.of("%"), Optional.empty(), Optional.empty(), 10))));
    }

    @Test
    void activeAtIsInclusiveOfBothBounds() {
        assertEquals(List.of("t1"), activeAt(0));
        assertEquals(List.of("t1", "t2"), activeAt(5 * 3600));
        assertEquals(List.of("t1", "t2"), activeAt(10 * 3600));
        assertEquals(List.of("t2"), activeAt(11 * 3600));
        assertTrue(activeAt(16 * 3600).isEmpty());
    }

    @Test
    void combinesFiltersAndPaging() {
        Optional<Instant> at = Optional.of(BASE.plusSeconds(7 * 3600));
        TripPage first = repo.find(filter(Optional.of("paris"), at, Optional.empty(), 1));
        assertEquals(List.of("t1"), ids(first));
        TripPage second = repo.find(filter(Optional.of("paris"), at, first.next(), 1));
        assertEquals(List.of("t2"), ids(second));
        assertTrue(second.next().isEmpty());
    }

    @Test
    void updatesStageAndPersistsIt() {
        Trip updated = repo.updateStage("t1", Stage.COMPLETED);
        assertEquals(Stage.COMPLETED, updated.stage());
        assertEquals("Paris Express", updated.name());
        TripPage page = repo.find(filter(Optional.of("express"), Optional.empty(), Optional.empty(), 10));
        assertEquals(Stage.COMPLETED, page.trips().get(0).stage());
    }

    @Test
    void updateStageOfMissingTripThrowsNotFound() {
        assertThrows(TripNotFoundException.class, () -> repo.updateStage("nope", Stage.COMPLETED));
    }

    private List<String> activeAt(long secondsAfterBase) {
        return ids(repo.find(filter(
                Optional.empty(), Optional.of(BASE.plusSeconds(secondsAfterBase)), Optional.empty(), 10)));
    }

    private static TripFilter filter(
            Optional<String> title, Optional<Instant> at, Optional<PageCursor> after, int size) {
        return new TripFilter(title, at, after, size);
    }

    private static List<String> ids(TripPage page) {
        return page.trips().stream().map(Trip::id).toList();
    }

    private static Mutation row(String id, String name, Stage stage, long createdOffset, long startHour, long endHour) {
        return Mutation.newInsertBuilder("Trips")
                .set("TripId").to(id)
                .set("Name").to(name)
                .set("Stage").to(stage.name())
                .set("IsActive").to(true)
                .set("StartTime").to(ts(BASE.plusSeconds(startHour * 3600)))
                .set("EndTime").to(ts(BASE.plusSeconds(endHour * 3600)))
                .set("CreatedAt").to(ts(BASE.minusSeconds(100 - createdOffset)))
                .build();
    }

    private static Timestamp ts(Instant i) {
        return Timestamp.ofTimeSecondsAndNanos(i.getEpochSecond(), i.getNano());
    }

    private static List<String> schemaStatements() throws Exception {
        String ddl = Files.readString(Path.of("spanner", "schema.sql"));
        return Arrays.stream(ddl.split(";")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    private static void post(String url, Object body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(body)))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("emulator setup failed: " + response.body());
        }
    }
}
