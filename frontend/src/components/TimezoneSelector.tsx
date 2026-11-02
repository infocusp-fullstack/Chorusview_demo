import { TIMEZONES } from "../time/time";
import { useTimezone } from "../time/timezoneContext";

export function TimezoneSelector() {
  const { zone, setZone } = useTimezone();
  return (
    <label className="field">
      <span>Timezone</span>
      <select value={zone} onChange={(e) => setZone(e.target.value)}>
        {TIMEZONES.map((tz) => (
          <option key={tz} value={tz}>
            {tz}
          </option>
        ))}
      </select>
    </label>
  );
}
