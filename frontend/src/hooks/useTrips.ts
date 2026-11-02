import { useCallback, useEffect, useMemo, useState } from "react";
import type { TripsApi } from "../api/tripsApi";
import type { Trip, TripStage } from "../api/types";

export interface TripFilters {
  searchTitle: string;
  activeAtTime?: string;
}

const PAGE_SIZE = 10;

/** Loads trips for the filters, with a token stack for Previous/Next paging. */
export function useTrips(api: TripsApi, filters: TripFilters) {
  const { searchTitle, activeAtTime } = filters;
  const filterKey = `${searchTitle}|${activeAtTime ?? ""}`;
  // Tokens of the pages visited so far; "" is the first page. A filter change starts a new stack.
  const [paging, setPaging] = useState({ key: filterKey, stack: [""] });
  const stack = useMemo(() => (paging.key === filterKey ? paging.stack : [""]), [paging, filterKey]);
  const token = stack[stack.length - 1];

  const [trips, setTrips] = useState<Trip[]>([]);
  const [nextToken, setNextToken] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    // Ignore results that arrive after the request was superseded or unmounted.
    const ifCurrent =
      <T>(handler: (value: T) => void) =>
      (value: T) => {
        if (!controller.signal.aborted) handler(value);
      };
    setLoading(true);
    setError(null);
    api
      .listTrips({ searchTitle, activeAtTime, pageToken: token, pageSize: PAGE_SIZE }, controller.signal)
      .then(
        ifCurrent((result) => {
          setTrips(result.trips);
          setNextToken(result.next_page_token);
          setLoading(false);
        }),
        ifCurrent((e: Error) => {
          setError(e.message);
          setLoading(false);
        }),
      );
    return () => controller.abort();
  }, [api, searchTitle, activeAtTime, token]);

  const next = useCallback(
    () => setPaging({ key: filterKey, stack: [...stack, nextToken] }),
    [filterKey, stack, nextToken],
  );
  const previous = useCallback(
    () => setPaging({ key: filterKey, stack: stack.slice(0, -1) }),
    [filterKey, stack],
  );

  const updateStage = useCallback(
    async (tripId: string, stage: TripStage) => {
      setUpdatingId(tripId);
      setError(null);
      try {
        const updated = await api.updateStage(tripId, stage);
        setTrips((current) => current.map((t) => (t.trip_id === tripId ? updated : t)));
      } catch (e) {
        setError((e as Error).message);
      } finally {
        setUpdatingId(null);
      }
    },
    [api],
  );

  return {
    trips,
    loading,
    error,
    updatingId,
    page: stack.length,
    hasNext: nextToken !== "",
    hasPrevious: stack.length > 1,
    next,
    previous,
    updateStage,
  };
}
