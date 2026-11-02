import { formatInZone } from "../time/time";

interface Props {
  search: string;
  activeAt: string;
  zone: string;
  /** The UTC instant sent to the API for the current "active at" input, if any. */
  activeAtUtc?: string;
  canClear: boolean;
  onSearchChange: (value: string) => void;
  onActiveAtChange: (value: string) => void;
  onClear: () => void;
}

export function TripFilters({
  search,
  activeAt,
  zone,
  activeAtUtc,
  canClear,
  onSearchChange,
  onActiveAtChange,
  onClear,
}: Props) {
  return (
    <div className="filters">
      <label className="field">
        <span>Search title</span>
        <input
          type="search"
          placeholder="e.g. Paris"
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </label>
      <label className="field">
        <span>Active at</span>
        <input type="datetime-local" value={activeAt} onChange={(e) => onActiveAtChange(e.target.value)} />
      </label>
      <button type="button" onClick={onClear} disabled={!canClear}>
        Clear filters
      </button>
      <p className="hint">
        {activeAtUtc
          ? `Read as ${zone} time = ${formatInZone(activeAtUtc, "UTC")}`
          : `Date and time are read in ${zone}`}
      </p>
    </div>
  );
}
