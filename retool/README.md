# Retool setup

Retool is built in its web UI. This file holds everything to paste in. The app's JSON export option was not
available, so the setup is documented here and in [screenshots/](screenshots/) instead.

## 1. REST API resource
Retool Cloud cannot reach `localhost`, so expose Envoy publicly with a tunnel. This setup used a Cloudflare quick tunnel (no account needed); ngrok works the same way:

```
cloudflared tunnel --url http://localhost:8080
# or: ngrok http 8080
```

The tunnel prints an `https://...` address; use it as the Base URL below. The address changes every time the tunnel restarts, so update the resource when that happens.

**Resources → Create new → REST API**
- Name: `tripsApi`
- Base URL: the tunnel's HTTPS URL
- Headers: `Content-Type: application/json`

## 2. Queries

### `listTrips` (run on page load)
- Method / URL: `GET /v1/trips`
- URL params:

| Key | Value |
|---|---|
| `page_size` | `100` |
| `search_title` | `{{ searchInput.value }}` |

### `updateStage` (manual)
- Method / URL: `PATCH /v1/trips/{{ tripsTable.selectedRow.data.trip_id }}/stage`
- Body (raw JSON): `{ "stage": "{{ newStage }}" }`
- Success handler: run `listTrips`

Buttons pass the target stage as a scope variable:

```js
updateStage.trigger({ additionalScope: { newStage: "COMPLETED" } });
```

## 3. Transformer `filteredTrips`

```js
const trips = listTrips.data.trips || [];
return trips.filter(
  (t) =>
    (!stageFilter.value || t.stage === stageFilter.value) &&
    (!statusFilter.value ||
      (statusFilter.value === "active") === t.is_active)
);
```

## 4. Components

| Component | Type | Settings |
|---|---|---|
| `searchInput` | Text Input | Event: change → run `listTrips` |
| `stageFilter` | Select | Values `NOT_STARTED`, `IN_TRANSIT`, `COMPLETED`; labels Not Started / In Transit / Completed; allow clear |
| `statusFilter` | Select | Values `active`, `inactive`; allow clear |
| `tripsTable` | Table | Data: `{{ filteredTrips.value }}`; single row selection |

## 5. Conditional panels
Each is a Container; set **Hidden** to the expression shown.

| Panel | Hidden when | Contents |
|---|---|---|
| Not started | `{{ tripsTable.selectedRow.data.stage !== 'NOT_STARTED' }}` | Start-trip button (`newStage = IN_TRANSIT`) |
| Operational controls | `{{ tripsTable.selectedRow.data.stage !== 'IN_TRANSIT' }}` | Mark completed (`COMPLETED`) and revert (`NOT_STARTED`) buttons |
| Archival summary | `{{ tripsTable.selectedRow.data.stage !== 'COMPLETED' }}` | Read-only text: name, start/end (UTC), duration, reopen button (`IN_TRANSIT`) |

Duration example for the summary text:

```js
{{ moment(tripsTable.selectedRow.data.end_time).diff(moment(tripsTable.selectedRow.data.start_time), 'hours') }} h
```

## As built
The dashboard was built with Retool's AI app builder and then verified by hand.

- Queries are JavaScript queries on the `tripsApi` resource (`rawRequest`) rather than the plain GET/PATCH editor described above. They call the same endpoints, `GET /v1/trips` and `PATCH /v1/trips/{trip_id}/stage`, with the same parameters and body.
- The queries send a browser-style `User-Agent` header. It was added for the Cloudflare quick tunnel that exposes the local API; the backend itself does not need it.
- Selecting a row opens a stage-specific panel: Start trip (Not Started), Operational controls (In Transit), Archival summary (Completed).
- The app only works while the tunnel and `docker compose --profile app up` are running.

## Screenshots
All files are in [screenshots/](screenshots/).

| File | Shows |
|---|---|
| `01-table.png` | All trips loaded through the REST API |
| `02-filter-in-transit.png` | Stage filter set to In Transit |
| `03-panel-not-started.png` | Not Started panel with the Start trip action |
| `04-panel-in-transit.png` | In Transit panel with Mark completed and Revert actions |
| `05-panel-completed.png` | Completed panel with the archival summary and Reopen action |
| `06-query-listTrips.png` | `listTrips` query: path, method and parameters |
| `07-query-updateStage.png` | `updateStage` query: method, path and body |
| `08a-stage-change-before.png` | Ahmedabad to Surat is Not Started |
| `08b-stage-change-panel.png` | Start trip panel opened for that trip |
| `08c-stage-change-after.png` | Ahmedabad to Surat is now In Transit |
