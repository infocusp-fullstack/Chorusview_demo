# Trip Management System

```
proto/      gRPC contract + google.api.http annotations (trips/v1/trips.proto)
backend/    Java gRPC service (Maven), Dockerfile, Spanner schema + seed (spanner/)
envoy/      Envoy gRPC-JSON transcoding config (REST -> gRPC)
frontend/   React + TypeScript (Vite)
retool/     Retool setup (queries, transformer, panels)
docs/       DECISIONS.md and DIAGRAMS.md (architecture, ER, flows)
docker-compose.yml
```

## Prerequisites
Docker, JDK 17, Maven 3.9+, Node 18+.

## 1. Start the Spanner emulator, schema and sample data
```
docker compose up -d spanner spanner-init
```
Creates instance `trips-instance` and database `trips-db`, applies [backend/spanner/schema.sql](backend/spanner/schema.sql) and loads [backend/spanner/seed.sql](backend/spanner/seed.sql) (12 sample trips).
Emulator: gRPC `localhost:9010`, REST `localhost:9020`.

## 2. Compile protobufs
```
cd backend
mvn compile
```
Generates the Java/gRPC stubs and writes the descriptor `envoy/descriptors/trips.pb` used by the transcoder.

## 3. Run the backend + REST transcoding
```
docker compose --profile app up --build
```
- gRPC: `localhost:9090`
- REST (Envoy transcoding): `http://localhost:8080/v1/trips`

```
curl "http://localhost:8080/v1/trips?search_title=paris&page_size=5"
curl "http://localhost:8080/v1/trips?active_at_time=2026-11-01T00:00:00Z"
curl -X PATCH http://localhost:8080/v1/trips/00000000-0000-0000-0000-000000000005/stage \
     -H "Content-Type: application/json" -d '{"stage":"IN_TRANSIT"}'
```

Run the backend outside Docker (the emulator must be up) with
`SPANNER_EMULATOR_HOST=localhost:9010 SPANNER_PROJECT=trips-project SPANNER_INSTANCE=trips-instance SPANNER_DATABASE=trips-db GRPC_PORT=9090 java -jar target/trips-backend.jar` (after `mvn package`).

## 4. Frontend
```
cd frontend
cp .env.example .env
npm install
npm run dev
```
Open http://localhost:5173 (`/v1` is proxied to Envoy on :8080).

## 5. Retool
See [retool/README.md](retool/README.md).

## Tests and quality gates
```
cd backend  && mvn clean verify    # unit + emulator tests (needs Docker), -Xlint:all -Werror, 100% coverage gate
cd frontend && npm run lint && npm test && npm run build
```

## Troubleshooting
- **`protoc did not exit cleanly` / "directory does not exist":** another tool (for example an IDE's Java extension) is building in `backend/target` at the same time. Close it or re-run `mvn clean verify`.
- **"Coverage checks have not been met" with "execution data does not match" warnings:** stale build output. Use `mvn clean verify`.
- **Backend returns `Database does not exist`:** the emulator keeps data in memory. After restarting it, run `docker compose --profile app up -d --force-recreate spanner-init backend envoy`.
- **Multi-word search returns nothing from your own client:** send spaces as `%20`, not `+`.

## Caveats
- **Close the IDE's Java extension before `mvn clean`.** If it builds in `backend/target` at the same time, protoc fails with "directory does not exist" and a missing `google/api/annotations.proto`. The code is fine; close the IDE (or its Java extension) and re-run `mvn clean verify`.
- **No CORS on Envoy.** The React app reaches the API through the Vite `/v1` proxy and Retool calls it from its own servers, so neither needs CORS. A browser app on another origin calling `localhost:8080` directly would be blocked (preflight returns 415). Add Envoy's CORS filter with an explicit origin list if that is ever needed.

## Design notes
Decisions and their status: [docs/DECISIONS.md](docs/DECISIONS.md). Diagrams: [docs/DIAGRAMS.md](docs/DIAGRAMS.md).
