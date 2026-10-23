-- Reference DDL (PostgreSQL). Hibernate generates the equivalent schema automatically (ddl-auto=update).

CREATE TABLE parking_floor (
    id            BIGSERIAL PRIMARY KEY,
    floor_number  INT NOT NULL UNIQUE
);

CREATE TABLE parking_spot (
    id            BIGSERIAL PRIMARY KEY,
    spot_number   VARCHAR(32) NOT NULL UNIQUE,
    spot_type     VARCHAR(16) NOT NULL CHECK (spot_type IN ('SMALL','MEDIUM','LARGE')),
    floor_id      BIGINT NOT NULL REFERENCES parking_floor(id),
    floor_number  INT NOT NULL,              -- denormalized for join-free allocation queries
    occupied      BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_spot_type_occupied ON parking_spot (spot_type, occupied);

CREATE TABLE vehicle (
    id             BIGSERIAL PRIMARY KEY,
    license_plate  VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type   VARCHAR(16) NOT NULL CHECK (vehicle_type IN ('MOTORCYCLE','CAR','BUS'))
);

CREATE TABLE fee_rule (
    id            BIGSERIAL PRIMARY KEY,
    vehicle_type  VARCHAR(16) NOT NULL UNIQUE,
    hourly_rate   NUMERIC(10,2) NOT NULL,
    daily_cap     NUMERIC(10,2) NOT NULL,
    grace_minutes INT NOT NULL
);

CREATE TABLE parking_ticket (
    id                 BIGSERIAL PRIMARY KEY,
    ticket_number      VARCHAR(40) NOT NULL UNIQUE,
    vehicle_id         BIGINT NOT NULL REFERENCES vehicle(id),
    spot_id            BIGINT NOT NULL REFERENCES parking_spot(id),
    vehicle_type       VARCHAR(16) NOT NULL,
    entry_time         TIMESTAMPTZ NOT NULL,
    exit_time          TIMESTAMPTZ,
    fee                NUMERIC(12,2),
    status             VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','COMPLETED')),
    active_vehicle_id  BIGINT UNIQUE          -- = vehicle_id while ACTIVE, NULL after: one active session per vehicle
);
CREATE INDEX idx_ticket_status ON parking_ticket (status);
