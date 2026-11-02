import { DateTime } from "luxon";

export const TIMEZONES = ["UTC", "America/New_York", "Asia/Kolkata", "Europe/London"];

/** Reads a datetime-local value as wall-clock time in `zone` and returns a UTC ISO-8601 string. */
export function localToUtcIso(local: string, zone: string): string | undefined {
  const parsed = DateTime.fromISO(local, { zone });
  return parsed.isValid ? parsed.toUTC().toISO({ suppressMilliseconds: true }) : undefined;
}

/** Renders a UTC ISO-8601 instant in `zone`. */
export function formatInZone(iso: string, zone: string): string {
  return DateTime.fromISO(iso, { zone: "utc" }).setZone(zone).toFormat("dd LLL yyyy, HH:mm (ZZZZ)");
}
