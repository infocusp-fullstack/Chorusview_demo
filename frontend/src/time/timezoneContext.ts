import { createContext, useContext } from "react";

export interface TimezoneState {
  zone: string;
  setZone: (zone: string) => void;
}

export const TimezoneContext = createContext<TimezoneState | null>(null);

export function useTimezone(): TimezoneState {
  const state = useContext(TimezoneContext);
  if (!state) {
    throw new Error("useTimezone must be used inside TimezoneProvider");
  }
  return state;
}
