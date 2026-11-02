import { afterEach, describe, expect, it, vi } from "vitest";
import { createTripsApi } from "./tripsApi";
import { makeTrip, page } from "../test/fakes";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

describe("createTripsApi", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("sends only the provided filters as query parameters", async () => {
    const fetchFn = vi.fn(() => Promise.resolve(jsonResponse(page([makeTrip()], "tok"))));
    const signal = new AbortController().signal;

    const result = await createTripsApi(fetchFn, "http://api").listTrips(
      { searchTitle: "paris", activeAtTime: "2026-11-01T04:30:00Z", pageToken: "abc", pageSize: 10 },
      signal,
    );

    expect(fetchFn).toHaveBeenCalledWith(
      "http://api/v1/trips?search_title=paris&active_at_time=2026-11-01T04%3A30%3A00Z&page_token=abc&page_size=10",
      { signal },
    );
    expect(result.next_page_token).toBe("tok");
  });

  it("encodes spaces as %20 and keeps a literal plus as %2B", async () => {
    const fetchFn = vi.fn(() => Promise.resolve(jsonResponse(page([]))));
    await createTripsApi(fetchFn).listTrips({ searchTitle: "Mumbai to Pune & A+B" }, new AbortController().signal);
    expect(fetchFn).toHaveBeenCalledWith(
      "/v1/trips?search_title=Mumbai%20to%20Pune%20%26%20A%2BB",
      expect.anything(),
    );
  });

  it("omits empty filters", async () => {
    const fetchFn = vi.fn(() => Promise.resolve(jsonResponse(page([]))));
    await createTripsApi(fetchFn).listTrips({ searchTitle: "", pageToken: "" }, new AbortController().signal);
    expect(fetchFn).toHaveBeenCalledWith("/v1/trips?", expect.anything());
  });

  it("patches the stage with a JSON body", async () => {
    const fetchFn = vi.fn(() => Promise.resolve(jsonResponse(makeTrip({ stage: "COMPLETED" }))));

    const trip = await createTripsApi(fetchFn).updateStage("a/b", "COMPLETED");

    expect(fetchFn).toHaveBeenCalledWith("/v1/trips/a%2Fb/stage", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: '{"stage":"COMPLETED"}',
    });
    expect(trip.stage).toBe("COMPLETED");
  });

  it("throws on a non-2xx response", async () => {
    const fetchFn = vi.fn(() => Promise.resolve(jsonResponse({}, 404)));
    await expect(createTripsApi(fetchFn).updateStage("x", "COMPLETED")).rejects.toThrow("Request failed (404)");
  });

  it("uses the global fetch by default", async () => {
    const globalFetch = vi.fn(() => Promise.resolve(jsonResponse(page([]))));
    vi.stubGlobal("fetch", globalFetch);
    await createTripsApi().listTrips({}, new AbortController().signal);
    expect(globalFetch).toHaveBeenCalledOnce();
  });
});
