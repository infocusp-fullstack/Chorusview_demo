# Diagrams

Mermaid diagrams (render on GitHub or in the VS Code Markdown preview).

## 1. Architecture

```mermaid
flowchart LR
    U[Customer browser<br/>React + TypeScript] -->|REST /v1/*| E[Envoy<br/>gRPC-JSON transcoder]
    R[Retool dashboard] -->|REST via ngrok| E
    E -->|gRPC / HTTP2| B[Java gRPC service<br/>TripGrpcService]
    B --> S[TripService]
    S --> P[(TripRepository)]
    P --> D[(Cloud Spanner<br/>emulator)]
    PB[trips.proto] -.->|protoc| B
    PB -.->|descriptor set trips.pb| E
```

## 2. Entity

```mermaid
erDiagram
    TRIPS {
        STRING_36 TripId PK "UUID"
        STRING_255 Name "index TripsByName"
        STRING_20 Stage "NOT_STARTED | IN_TRANSIT | COMPLETED"
        BOOL IsActive "index TripsByActiveName"
        TIMESTAMP StartTime "UTC, index TripsByStartTime"
        TIMESTAMP EndTime "UTC"
        TIMESTAMP CreatedAt "UTC, keyset order"
    }
```

## 3. Stage lifecycle

Any stage can be set to any other (operations can override); the usual flow is:

```mermaid
stateDiagram-v2
    [*] --> NOT_STARTED
    NOT_STARTED --> IN_TRANSIT: start
    IN_TRANSIT --> COMPLETED: finish
    COMPLETED --> IN_TRANSIT: reopen (override)
    IN_TRANSIT --> NOT_STARTED: revert (override)
```

## 4. API flow: ListTrips

```mermaid
sequenceDiagram
    participant C as Client
    participant E as Envoy
    participant G as TripGrpcService
    participant S as TripService
    participant R as SpannerTripRepository
    participant DB as Spanner
    C->>E: GET /v1/trips?search_title&active_at_time&page_token
    E->>G: ListTrips(request)
    G->>S: listTrips(title, activeAt, size, token)
    S->>S: validate, parse UTC time, decode token, clamp page size
    S->>R: find(TripFilter)
    R->>DB: SELECT ... WHERE LOWER(Name) LIKE, time range, keyset ORDER BY CreatedAt, TripId LIMIT n+1
    DB-->>R: rows
    R-->>S: TripPage (n rows + next cursor)
    S-->>G: TripListing (next token encoded)
    G-->>E: ListTripsResponse
    E-->>C: 200 JSON (400 on invalid input)
```

## 5. API flow: UpdateTripStage

```mermaid
sequenceDiagram
    participant C as Client
    participant E as Envoy
    participant G as TripGrpcService
    participant S as TripService
    participant R as SpannerTripRepository
    participant DB as Spanner
    C->>E: PATCH /v1/trips/{id}/stage {"stage":"IN_TRANSIT"}
    E->>G: UpdateTripStage
    G->>G: map stage (reject UNSPECIFIED)
    G->>S: updateStage(id, stage)
    S->>R: updateStage(id, stage)
    R->>DB: read-write txn: readRow, buffer update
    alt trip exists
        DB-->>R: committed
        R-->>G: updated Trip
        G-->>C: 200 Trip
    else unknown id
        R-->>G: TripNotFoundException
        G-->>C: 404
    end
```

## 6. Timezone flow (frontend)

```mermaid
flowchart TD
    A[User picks timezone] --> Z[(TimezoneContext)]
    B[User enters local date-time] --> L[localToUtcIso value, zone]
    Z --> L
    L -->|UTC ISO-8601| Q[active_at_time query param]
    Q --> API[GET /v1/trips]
    API --> T[Trips with UTC timestamps]
    T --> F[formatInZone iso, zone]
    Z --> F
    F --> UI[Start / End shown in selected timezone]
```

## 7. Customer user flow

```mermaid
flowchart TD
    S([Open app]) --> LOAD[First page of trips loads, UTC shown]
    LOAD --> TZ{Change timezone?}
    TZ -->|yes| RENDER[All times re-rendered; active-at filter re-evaluated]
    TZ -->|no| ACT
    RENDER --> ACT{Action}
    ACT -->|type in search| DEB[300 ms debounce] --> FETCH[Refetch from page 1]
    ACT -->|pick active-at date-time| FETCH
    ACT -->|change stage dropdown| PATCH[PATCH stage] --> ROW[Row badge updates]
    ACT -->|Next / Previous| PAGE[Fetch page by token]
    FETCH --> LOAD
    PAGE --> LOAD
```

## 8. Backend structure

```mermaid
classDiagram
    class TripGrpcService {
        +listTrips()
        +updateTripStage()
    }
    class TripService {
        +listTrips() TripListing
        +updateStage() Trip
    }
    class PageTokenCodec {
        +encode(PageCursor) String
        +decode(String) PageCursor
    }
    class TripRepository {
        <<interface>>
        +find(TripFilter) TripPage
        +updateStage(String, Stage) Trip
    }
    class SpannerTripRepository
    class TripMapper
    TripGrpcService --> TripService
    TripGrpcService --> TripMapper
    TripService --> TripRepository
    TripService --> PageTokenCodec
    SpannerTripRepository ..|> TripRepository
```
