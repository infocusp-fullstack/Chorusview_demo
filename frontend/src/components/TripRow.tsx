import type { Trip, TripStage } from "../api/types";
import { formatInZone } from "../time/time";
import { useTimezone } from "../time/timezoneContext";
import { StageBadge } from "./StageBadge";
import { StageUpdater } from "./StageUpdater";

interface Props {
  trip: Trip;
  updating: boolean;
  onStageChange: (tripId: string, stage: TripStage) => void;
}

export function TripRow({ trip, updating, onStageChange }: Props) {
  const { zone } = useTimezone();
  return (
    <li className="trip">
      <div>
        <h3 className="trip-name">{trip.name}</h3>
        <div className="trip-tags">
          <StageBadge stage={trip.stage} />
          <span className={trip.is_active ? "status active" : "status"}>
            {trip.is_active ? "Active" : "Inactive"}
          </span>
        </div>
      </div>
      <dl className="trip-times">
        <div>
          <dt>Start</dt>
          <dd>{formatInZone(trip.start_time, zone)}</dd>
        </div>
        <div>
          <dt>End</dt>
          <dd>{formatInZone(trip.end_time, zone)}</dd>
        </div>
      </dl>
      <StageUpdater stage={trip.stage} disabled={updating} onChange={(s) => onStageChange(trip.trip_id, s)} />
    </li>
  );
}
