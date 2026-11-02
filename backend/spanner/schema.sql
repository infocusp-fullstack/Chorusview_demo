CREATE TABLE Trips (
  TripId STRING(36) NOT NULL,
  Name STRING(255) NOT NULL,
  Stage STRING(20) NOT NULL,
  IsActive BOOL NOT NULL,
  StartTime TIMESTAMP NOT NULL,
  EndTime TIMESTAMP NOT NULL,
  CreatedAt TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp = true),
  CONSTRAINT CK_Trips_Stage CHECK (Stage IN ('NOT_STARTED', 'IN_TRANSIT', 'COMPLETED'))
) PRIMARY KEY (TripId);

CREATE INDEX TripsByName ON Trips(Name);

CREATE INDEX TripsByActiveName ON Trips(IsActive, Name);

CREATE INDEX TripsByStartTime ON Trips(StartTime) STORING (EndTime);
