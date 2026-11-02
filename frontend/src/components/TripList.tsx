import { useMemo, useState } from "react";
import type { TripsApi } from "../api/tripsApi";
import { useDebouncedValue } from "../hooks/useDebouncedValue";
import { useTrips } from "../hooks/useTrips";
import { localToUtcIso } from "../time/time";
import { useTimezone } from "../time/timezoneContext";
import { Pagination } from "./Pagination";
import { TripFilters } from "./TripFilters";
import { TripRow } from "./TripRow";

export const SEARCH_DEBOUNCE_MS = 300;

export function TripList({ api }: { api: TripsApi }) {
  const { zone } = useTimezone();
  const [search, setSearch] = useState("");
  const [activeAtLocal, setActiveAtLocal] = useState("");
  const searchTitle = useDebouncedValue(search.trim(), SEARCH_DEBOUNCE_MS);
  // Re-evaluated whenever the global timezone changes.
  const activeAtTime = useMemo(() => localToUtcIso(activeAtLocal, zone), [activeAtLocal, zone]);

  const { trips, loading, error, updatingId, page, hasNext, hasPrevious, next, previous, updateStage } = useTrips(
    api,
    { searchTitle, activeAtTime },
  );

  const clearFilters = () => {
    setSearch("");
    setActiveAtLocal("");
  };

  return (
    <section>
      <TripFilters
        search={search}
        activeAt={activeAtLocal}
        zone={zone}
        activeAtUtc={activeAtTime}
        canClear={search !== "" || activeAtLocal !== ""}
        onSearchChange={setSearch}
        onActiveAtChange={setActiveAtLocal}
        onClear={clearFilters}
      />
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      {loading && (
        <p className="status-message" role="status">
          Loading…
        </p>
      )}
      {!loading && trips.length === 0 && !error && <p className="status-message">No trips found.</p>}
      <ul className={loading ? "trips is-loading" : "trips"} aria-busy={loading}>
        {trips.map((trip) => (
          <TripRow key={trip.trip_id} trip={trip} updating={updatingId === trip.trip_id} onStageChange={updateStage} />
        ))}
      </ul>
      <Pagination page={page} hasPrevious={hasPrevious} hasNext={hasNext} onPrevious={previous} onNext={next} />
    </section>
  );
}
