package com.infocusp.trips.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.StructReader;
import com.infocusp.trips.domain.PageCursor;
import com.infocusp.trips.domain.Stage;
import com.infocusp.trips.domain.Trip;
import com.infocusp.trips.domain.TripFilter;
import com.infocusp.trips.domain.TripNotFoundException;
import com.infocusp.trips.domain.TripPage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Cloud Spanner implementation of the trip repository. */
public final class SpannerTripRepository implements TripRepository {
    private static final String TABLE = "Trips";
    private static final List<String> COLUMNS =
            List.of("TripId", "Name", "Stage", "IsActive", "StartTime", "EndTime", "CreatedAt");
    private static final String SELECT = "SELECT " + String.join(", ", COLUMNS) + " FROM " + TABLE;

    private final DatabaseClient client;

    public SpannerTripRepository(DatabaseClient client) {
        this.client = client;
    }

    @Override
    public TripPage find(TripFilter filter) {
        Statement.Builder query = Statement.newBuilder(SELECT);
        String joiner = " WHERE ";
        if (filter.searchTitle().isPresent()) {
            query.append(joiner + "LOWER(Name) LIKE @name")
                    .bind("name").to(likePattern(filter.searchTitle().get()));
            joiner = " AND ";
        }
        if (filter.activeAt().isPresent()) {
            query.append(joiner + "StartTime <= @at AND EndTime >= @at")
                    .bind("at").to(toTimestamp(filter.activeAt().get()));
            joiner = " AND ";
        }
        if (filter.after().isPresent()) {
            PageCursor c = filter.after().get();
            query.append(joiner + "(CreatedAt > @cAt OR (CreatedAt = @cAt AND TripId > @cId))")
                    .bind("cAt").to(toTimestamp(c.createdAt()))
                    .bind("cId").to(c.tripId());
        }
        // One extra row tells us whether another page exists.
        query.append(" ORDER BY CreatedAt, TripId LIMIT @limit")
                .bind("limit").to((long) filter.pageSize() + 1);

        List<Trip> trips = new ArrayList<>();
        try (ResultSet rs = client.singleUse().executeQuery(query.build())) {
            while (rs.next()) {
                trips.add(toTrip(rs));
            }
        }
        if (trips.size() <= filter.pageSize()) {
            return new TripPage(trips, Optional.empty());
        }
        List<Trip> page = trips.subList(0, filter.pageSize());
        Trip last = page.get(page.size() - 1);
        return new TripPage(page, Optional.of(new PageCursor(last.createdAt(), last.id())));
    }

    @Override
    public Trip updateStage(String tripId, Stage stage) {
        Optional<Trip> updated = client.readWriteTransaction().run(txn -> {
            Struct row = txn.readRow(TABLE, Key.of(tripId), COLUMNS);
            if (row == null) {
                return Optional.empty();
            }
            txn.buffer(Mutation.newUpdateBuilder(TABLE)
                    .set("TripId").to(tripId)
                    .set("Stage").to(stage.name())
                    .build());
            return Optional.of(toTrip(row).withStage(stage));
        });
        return updated.orElseThrow(() -> new TripNotFoundException(tripId));
    }

    /** Lower-cased substring pattern with LIKE wildcards escaped. */
    private static String likePattern(String title) {
        String escaped = title.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static Trip toTrip(StructReader r) {
        return new Trip(
                r.getString("TripId"),
                r.getString("Name"),
                Stage.valueOf(r.getString("Stage")),
                r.getBoolean("IsActive"),
                toInstant(r.getTimestamp("StartTime")),
                toInstant(r.getTimestamp("EndTime")),
                toInstant(r.getTimestamp("CreatedAt")));
    }

    private static Instant toInstant(Timestamp t) {
        return Instant.ofEpochSecond(t.getSeconds(), t.getNanos());
    }

    private static Timestamp toTimestamp(Instant i) {
        return Timestamp.ofTimeSecondsAndNanos(i.getEpochSecond(), i.getNano());
    }
}
