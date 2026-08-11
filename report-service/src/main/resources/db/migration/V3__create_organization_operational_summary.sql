CREATE TABLE report.organization_operational_summary (
    organization_id UUID PRIMARY KEY,
    station_count INT NOT NULL DEFAULT 0,
    total_flights_last_30_days INT NOT NULL DEFAULT 0,
    last_flight_activity_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
