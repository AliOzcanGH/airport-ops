CREATE TABLE flight.flights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    flight_number VARCHAR(10) NOT NULL,
    origin VARCHAR(10) NOT NULL,
    destination VARCHAR(10) NOT NULL,
    scheduled_departure TIMESTAMPTZ NOT NULL,
    scheduled_arrival TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    assigned_gate_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organization_id, flight_number, scheduled_departure)
);

CREATE INDEX idx_flights_organization_id ON flight.flights (organization_id);
CREATE INDEX idx_flights_assigned_gate_id ON flight.flights (assigned_gate_id);
