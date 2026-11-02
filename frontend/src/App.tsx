import { createTripsApi } from "./api/tripsApi";
import { TimezoneSelector } from "./components/TimezoneSelector";
import { TripList } from "./components/TripList";
import { TimezoneProvider } from "./time/TimezoneProvider";

const api = createTripsApi();

export function App() {
  return (
    <TimezoneProvider>
      <header className="app-header">
        <div>
          <h1>Trip Management</h1>
          <p className="subtitle">Browse trips and update their stage</p>
        </div>
        <TimezoneSelector />
      </header>
      <main>
        <TripList api={api} />
      </main>
    </TimezoneProvider>
  );
}
