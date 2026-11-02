import { vi } from "vitest";
import type { TripsApi } from "../api/tripsApi";
import type { ListTripsResult, Trip } from "../api/types";

export function makeTrip(overrides: Partial<Trip> = {}): Trip {
  return {
    trip_id: "t1",
    name: "Paris Express",
    stage: "NOT_STARTED",
    is_active: true,
    start_time: "2026-11-01T04:30:00Z",
    end_time: "2026-11-02T04:30:00Z",
    created_at: "2026-10-01T00:00:00Z",
    ...overrides,
  };
}

export function page(trips: Trip[], nextToken = ""): ListTripsResult {
  return { trips, next_page_token: nextToken };
}

export function fakeApi(initial: ListTripsResult = page([makeTrip()])) {
  const listTrips = vi.fn<TripsApi["listTrips"]>(() => Promise.resolve(initial));
  const updateStage = vi.fn<TripsApi["updateStage"]>((id, stage) =>
    Promise.resolve(makeTrip({ trip_id: id, stage })),
  );
  return { listTrips, updateStage } satisfies TripsApi;
}
