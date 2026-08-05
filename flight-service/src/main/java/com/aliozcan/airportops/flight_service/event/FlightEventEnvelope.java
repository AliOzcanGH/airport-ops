package com.aliozcan.airportops.flight_service.event;

import java.time.Instant;
import java.util.UUID;

public record FlightEventEnvelope(
        UUID eventId,
        String eventType,
        UUID organizationId,
        Instant occurredAt,
        Object payload) {
}
