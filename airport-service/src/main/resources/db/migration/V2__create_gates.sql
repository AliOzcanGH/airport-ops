CREATE TABLE airport.gates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    station_id UUID NOT NULL REFERENCES airport.stations(id),
    code VARCHAR(10) NOT NULL,
    terminal VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(station_id, code)
);

CREATE INDEX idx_gates_station_id ON airport.gates (station_id);
