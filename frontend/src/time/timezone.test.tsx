import { act, renderHook } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";
import { TimezoneProvider } from "./TimezoneProvider";
import { useTimezone } from "./timezoneContext";

describe("timezone context", () => {
  it("defaults to UTC and updates the zone", () => {
    const wrapper = ({ children }: { children: ReactNode }) => <TimezoneProvider>{children}</TimezoneProvider>;
    const { result } = renderHook(() => useTimezone(), { wrapper });
    expect(result.current.zone).toBe("UTC");
    act(() => result.current.setZone("Asia/Kolkata"));
    expect(result.current.zone).toBe("Asia/Kolkata");
  });

  it("throws when used outside the provider", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => undefined);
    expect(() => renderHook(() => useTimezone())).toThrow("TimezoneProvider");
    spy.mockRestore();
  });
});
