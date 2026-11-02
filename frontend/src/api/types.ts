export const STAGES = ["NOT_STARTED", "IN_TRANSIT", "COMPLETED"] as const;
export type TripStage = (typeof STAGES)[number];

export const STAGE_LABELS: Record<TripStage, string> = {
  NOT_STARTED: "Not Started",
  IN_TRANSIT: "In Transit",
  COMPLETED: "Completed",
};

// Field names match the transcoded REST JSON (proto field names).
export interface Trip {
  trip_id: string;
  name: string;
  stage: TripStage;
  is_active: boolean;
  start_time: string;
  end_time: string;
  created_at: string;
}

export interface ListTripsParams {
  searchTitle?: string;
  activeAtTime?: string;
  pageToken?: string;
  pageSize?: number;
}

export interface ListTripsResult {
  trips: Trip[];
  next_page_token: string;
}
