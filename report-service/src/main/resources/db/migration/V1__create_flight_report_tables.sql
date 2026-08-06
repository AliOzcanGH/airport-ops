CREATE TABLE report.flight_report_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flight_id UUID,
    organization_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_flight_report_entries_organization_id ON report.flight_report_entries (organization_id);
CREATE INDEX idx_flight_report_entries_flight_id ON report.flight_report_entries (flight_id);

CREATE TABLE report.processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
