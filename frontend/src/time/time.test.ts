import { describe, expect, it } from "vitest";
import { formatInZone, localToUtcIso } from "./time";

describe("localToUtcIso", () => {
  it("converts wall-clock time in the zone to UTC", () => {
    expect(localToUtcIso("2026-11-01T10:00", "Asia/Kolkata")).toBe("2026-11-01T04:30:00Z");
    expect(localToUtcIso("2026-11-01T10:00", "UTC")).toBe("2026-11-01T10:00:00Z");
    expect(localToUtcIso("2026-11-01T10:00", "Europe/London")).toBe("2026-11-01T10:00:00Z");
  });

  it("honours daylight saving rules (US DST ends on 2026-11-01)", () => {
    expect(localToUtcIso("2026-11-01T10:00", "America/New_York")).toBe("2026-11-01T15:00:00Z");
    expect(localToUtcIso("2026-10-31T10:00", "America/New_York")).toBe("2026-10-31T14:00:00Z");
  });

  it("returns undefined for empty or invalid input", () => {
    expect(localToUtcIso("", "UTC")).toBeUndefined();
    expect(localToUtcIso("not-a-date", "UTC")).toBeUndefined();
  });
});

describe("formatInZone", () => {
  it("renders the same instant in different zones", () => {
    expect(formatInZone("2026-11-01T04:30:00Z", "UTC")).toBe("01 Nov 2026, 04:30 (UTC)");
    expect(formatInZone("2026-11-01T04:30:00Z", "Asia/Kolkata")).toContain("01 Nov 2026, 10:00");
    expect(formatInZone("2026-11-01T04:30:00Z", "America/New_York")).toContain("01 Nov 2026, 00:30");
  });
});
