import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { ListTripsResult } from "../api/types";
import { fakeApi, makeTrip, page } from "../test/fakes";
import { TimezoneProvider } from "../time/TimezoneProvider";
import { TimezoneSelector } from "./TimezoneSelector";
import { TripList } from "./TripList";

function renderList(api: ReturnType<typeof fakeApi>) {
  return render(
    <TimezoneProvider>
      <TimezoneSelector />
      <TripList api={api} />
    </TimezoneProvider>,
  );
}

function lastParams(api: ReturnType<typeof fakeApi>) {
  const call = api.listTrips.mock.lastCall;
  if (!call) throw new Error("listTrips was not called");
  return call[0];
}

async function findRow(name: string): Promise<HTMLElement> {
  const row = (await screen.findByText(name)).closest("li");
  if (!row) throw new Error(`no row for ${name}`);
  return row;
}

function deferred<T>() {
  let resolve!: (v: T) => void;
  let reject!: (e: Error) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe("TripList", () => {
  it("shows loading, then trip details in the selected timezone", async () => {
    renderList(fakeApi());
    expect(screen.getByText("Loading…")).toBeInTheDocument();

    expect(await screen.findByText("Paris Express")).toBeInTheDocument();
    expect(screen.getByText("Not Started", { selector: "span" })).toBeInTheDocument();
    expect(screen.getByText("Active")).toBeInTheDocument();
    expect(screen.getByText("01 Nov 2026, 04:30 (UTC)")).toBeInTheDocument();
    expect(screen.getByText("Page 1")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Timezone"), { target: { value: "Asia/Kolkata" } });
    expect(screen.getByText(/^01 Nov 2026, 10:00/)).toBeInTheDocument();
    expect(screen.getByText(/^02 Nov 2026, 10:00/)).toBeInTheDocument();
  });

  it("explains how the active-at input is interpreted and lets the user clear filters", async () => {
    const api = fakeApi();
    renderList(api);
    await screen.findByText("Paris Express");
    const clear = screen.getByText("Clear filters");
    expect(clear).toBeDisabled();
    expect(screen.getByText("Date and time are read in UTC")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Timezone"), { target: { value: "Asia/Kolkata" } });
    fireEvent.change(screen.getByLabelText("Active at"), { target: { value: "2026-11-01T10:00" } });
    fireEvent.change(screen.getByLabelText("Search title"), { target: { value: "par" } });
    expect(screen.getByText("Read as Asia/Kolkata time = 01 Nov 2026, 04:30 (UTC)")).toBeInTheDocument();
    expect(clear).toBeEnabled();

    fireEvent.click(clear);
    expect(screen.getByLabelText("Search title")).toHaveValue("");
    expect(screen.getByLabelText("Active at")).toHaveValue("");
    expect(clear).toBeDisabled();
    await waitFor(() => expect(lastParams(api).activeAtTime).toBeUndefined());
  });

  it("marks inactive trips and shows an empty state", async () => {
    renderList(fakeApi(page([makeTrip({ is_active: false })])));
    expect(await screen.findByText("Inactive")).toBeInTheDocument();

    const empty = fakeApi(page([]));
    renderList(empty);
    expect(await screen.findByText("No trips found.")).toBeInTheDocument();
  });

  it("debounces the title search", async () => {
    const api = fakeApi();
    renderList(api);
    await screen.findByText("Paris Express");
    const callsBefore = api.listTrips.mock.calls.length;

    fireEvent.change(screen.getByLabelText("Search title"), { target: { value: "  par " } });
    expect(api.listTrips.mock.calls.length).toBe(callsBefore);

    await waitFor(() => expect(lastParams(api).searchTitle).toBe("par"), { timeout: 2000 });
    expect(api.listTrips.mock.calls.length).toBe(callsBefore + 1);
  });

  it("evaluates 'active at' in the selected timezone and sends UTC", async () => {
    const api = fakeApi();
    renderList(api);
    await screen.findByText("Paris Express");

    fireEvent.change(screen.getByLabelText("Timezone"), { target: { value: "Asia/Kolkata" } });
    fireEvent.change(screen.getByLabelText("Active at"), { target: { value: "2026-11-01T10:00" } });
    await waitFor(() => expect(lastParams(api).activeAtTime).toBe("2026-11-01T04:30:00Z"));

    fireEvent.change(screen.getByLabelText("Timezone"), { target: { value: "UTC" } });
    await waitFor(() => expect(lastParams(api).activeAtTime).toBe("2026-11-01T10:00:00Z"));

    fireEvent.change(screen.getByLabelText("Active at"), { target: { value: "" } });
    await waitFor(() => expect(lastParams(api).activeAtTime).toBeUndefined());
  });

  it("updates a trip's stage and disables the dropdown while saving", async () => {
    const api = fakeApi();
    const saving = deferred<ReturnType<typeof makeTrip>>();
    api.updateStage.mockImplementationOnce(() => saving.promise);
    renderList(api);
    const row = await findRow("Paris Express");

    fireEvent.change(within(row).getByLabelText("Change stage"), { target: { value: "IN_TRANSIT" } });
    expect(api.updateStage).toHaveBeenCalledWith("t1", "IN_TRANSIT");
    await waitFor(() => expect(within(row).getByLabelText("Change stage")).toBeDisabled());
    expect(within(row).getByText("Saving…")).toBeInTheDocument();

    saving.resolve(makeTrip({ stage: "IN_TRANSIT" }));
    expect(await within(row).findByText("In Transit", { selector: "span" })).toBeInTheDocument();
    expect(within(row).getByLabelText("Change stage")).toBeEnabled();
    expect(within(row).queryByText("Saving…")).not.toBeInTheDocument();
  });

  it("keeps other rows untouched when one trip is updated", async () => {
    const api = fakeApi(page([makeTrip(), makeTrip({ trip_id: "t2", name: "Rome" })]));
    renderList(api);
    const row = await findRow("Paris Express");

    fireEvent.change(within(row).getByLabelText("Change stage"), { target: { value: "COMPLETED" } });

    await waitFor(() => expect(within(row).getByText("Completed", { selector: "span" })).toBeInTheDocument());
    const other = await findRow("Rome");
    expect(within(other).getByText("Not Started", { selector: "span" })).toBeInTheDocument();
  });

  it("shows an error when the stage update fails", async () => {
    const api = fakeApi();
    api.updateStage.mockRejectedValueOnce(new Error("Request failed (500)"));
    renderList(api);
    const row = await findRow("Paris Express");

    fireEvent.change(within(row).getByLabelText("Change stage"), { target: { value: "COMPLETED" } });

    expect(await screen.findByRole("alert")).toHaveTextContent("Request failed (500)");
    expect(within(row).getByText("Not Started", { selector: "span" })).toBeInTheDocument();
  });

  it("shows an error when loading fails", async () => {
    const api = fakeApi();
    api.listTrips.mockRejectedValueOnce(new Error("Request failed (503)"));
    renderList(api);
    expect(await screen.findByRole("alert")).toHaveTextContent("Request failed (503)");
    expect(screen.queryByText("No trips found.")).not.toBeInTheDocument();
  });

  it("pages forward and back with server tokens", async () => {
    const api = fakeApi();
    api.listTrips.mockImplementation((params) =>
      Promise.resolve<ListTripsResult>(
        params.pageToken === "p2"
          ? page([makeTrip({ trip_id: "t2", name: "Page Two" })])
          : page([makeTrip({ name: "Page One" })], "p2"),
      ),
    );
    renderList(api);
    await screen.findByText("Page One");
    expect(screen.getByText("Previous")).toBeDisabled();

    fireEvent.click(screen.getByText("Next"));
    expect(await screen.findByText("Page Two")).toBeInTheDocument();
    expect(screen.getByText("Page 2")).toBeInTheDocument();
    expect(lastParams(api).pageToken).toBe("p2");
    expect(screen.getByText("Next")).toBeDisabled();

    fireEvent.click(screen.getByText("Previous"));
    expect(await screen.findByText("Page One")).toBeInTheDocument();
  });

  it("returns to the first page when a filter changes", async () => {
    const api = fakeApi(page([makeTrip()], "p2"));
    renderList(api);
    await screen.findByText("Paris Express");
    fireEvent.click(screen.getByText("Next"));
    await waitFor(() => expect(lastParams(api).pageToken).toBe("p2"));

    fireEvent.change(screen.getByLabelText("Active at"), { target: { value: "2026-11-01T10:00" } });

    await waitFor(() => expect(lastParams(api).activeAtTime).toBe("2026-11-01T10:00:00Z"));
    expect(lastParams(api).pageToken).toBe("");
    expect(screen.getByText("Previous")).toBeDisabled();
  });

  it("ignores a response that arrives after unmount", async () => {
    const api = fakeApi();
    const slow = deferred<ListTripsResult>();
    api.listTrips.mockImplementationOnce(() => slow.promise);
    const view = renderList(api);
    view.unmount();
    slow.resolve(page([makeTrip()]));
    await slow.promise;
    expect(screen.queryByText("Paris Express")).not.toBeInTheDocument();
  });

  it("ignores a failure that arrives after unmount", async () => {
    const api = fakeApi();
    const slow = deferred<ListTripsResult>();
    api.listTrips.mockImplementationOnce(() => slow.promise);
    const view = renderList(api);
    view.unmount();
    slow.reject(new Error("late"));
    await slow.promise.catch(() => undefined);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});
