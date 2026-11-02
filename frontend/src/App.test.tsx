import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";
import { makeTrip, page } from "./test/fakes";

describe("App", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("renders the header, timezone selector and trips from the REST API", async () => {
    const fetchMock = vi.fn(() => Promise.resolve(new Response(JSON.stringify(page([makeTrip()])))));
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);

    expect(screen.getByRole("heading", { name: "Trip Management" })).toBeInTheDocument();
    expect(screen.getByLabelText("Timezone")).toHaveValue("UTC");
    expect(await screen.findByText("Paris Express")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalled();
  });
});
