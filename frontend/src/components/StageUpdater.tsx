import { STAGES, STAGE_LABELS, type TripStage } from "../api/types";

interface Props {
  stage: TripStage;
  disabled: boolean;
  onChange: (stage: TripStage) => void;
}

export function StageUpdater({ stage, disabled, onChange }: Props) {
  return (
    <div className="stage-update">
      <label className="field">
        <span>Change stage</span>
        <select value={stage} disabled={disabled} onChange={(e) => onChange(e.target.value as TripStage)}>
          {STAGES.map((s) => (
            <option key={s} value={s}>
              {STAGE_LABELS[s]}
            </option>
          ))}
        </select>
      </label>
      {disabled && (
        <span className="saving" role="status">
          Saving…
        </span>
      )}
    </div>
  );
}
