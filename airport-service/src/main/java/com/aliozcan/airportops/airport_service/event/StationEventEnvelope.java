package com.aliozcan.airportops.airport_service.event;

import java.time.Instant;
import java.util.UUID;

public record StationEventEnvelope(
        UUID eventId,
        String eventType,
        UUID organizationId,
        Instant occurredAt,
        Object payload) {
}
