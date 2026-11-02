import { useMemo, useState, type ReactNode } from "react";
import { TimezoneContext } from "./timezoneContext";

export function TimezoneProvider({ children }: { children: ReactNode }) {
  const [zone, setZone] = useState("UTC");
  const value = useMemo(() => ({ zone, setZone }), [zone]);
  return <TimezoneContext.Provider value={value}>{children}</TimezoneContext.Provider>;
}
