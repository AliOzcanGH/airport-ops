CREATE TABLE airport.stations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    station_name VARCHAR(120) NOT NULL,
    airport_code VARCHAR(10) NOT NULL,
    gate_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_stations_organization_id ON airport.stations (organization_id);
