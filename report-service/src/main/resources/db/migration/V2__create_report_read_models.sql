CREATE TABLE report.daily_flight_summary (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    summary_date DATE NOT NULL,
    total_flights INT NOT NULL DEFAULT 0,
    delayed_flights INT NOT NULL DEFAULT 0,
    cancelled_flights INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organization_id, summary_date)
);

CREATE TABLE report.gate_utilization (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    gate_id UUID NOT NULL,
    summary_date DATE NOT NULL,
    flight_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organization_id, gate_id, summary_date)
);
