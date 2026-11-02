import type { ListTripsParams, ListTripsResult, Trip, TripStage } from "./types";

export interface TripsApi {
  listTrips(params: ListTripsParams, signal: AbortSignal): Promise<ListTripsResult>;
  updateStage(tripId: string, stage: TripStage): Promise<Trip>;
}

type FetchFn = (input: string, init?: RequestInit) => Promise<Response>;

async function request<T>(fetchFn: FetchFn, url: string, init?: RequestInit): Promise<T> {
  const response = await fetchFn(url, init);
  if (!response.ok) {
    throw new Error(`Request failed (${response.status})`);
  }
  return (await response.json()) as T;
}

/** REST client for the transcoded gRPC endpoints. */
export function createTripsApi(fetchFn: FetchFn = (input, init) => fetch(input, init), baseUrl = ""): TripsApi {
  return {
    listTrips(params, signal) {
      const entries: [string, string | number | undefined][] = [
        ["search_title", params.searchTitle],
        ["active_at_time", params.activeAtTime],
        ["page_token", params.pageToken],
        ["page_size", params.pageSize],
      ];
      const query = new URLSearchParams();
      entries.forEach(([key, value]) => value && query.set(key, String(value)));
      // The transcoder reads "+" literally, so spaces must be %20 (a real "+" is already %2B).
      const queryString = query.toString().replace(/\+/g, "%20");
      return request<ListTripsResult>(fetchFn, `${baseUrl}/v1/trips?${queryString}`, { signal });
    },
    updateStage(tripId, stage) {
      return request<Trip>(fetchFn, `${baseUrl}/v1/trips/${encodeURIComponent(tripId)}/stage`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ stage }),
      });
    },
  };
}
