import { STAGE_LABELS, type TripStage } from "../api/types";

export function StageBadge({ stage }: { stage: TripStage }) {
  return <span className={`badge badge-${stage}`}>{STAGE_LABELS[stage]}</span>;
}
