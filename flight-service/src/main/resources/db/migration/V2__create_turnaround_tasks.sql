CREATE TABLE flight.turnaround_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flight_id UUID NOT NULL REFERENCES flight.flights(id),
    task_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_to VARCHAR(150),
    due_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_turnaround_tasks_flight_id ON flight.turnaround_tasks (flight_id);
