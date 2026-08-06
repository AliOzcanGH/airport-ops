package com.aliozcan.airportops.audit_service.event;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record FlightEventEnvelope(
        UUID eventId,
        String eventType,
        UUID organizationId,
        String occurredAt,
        JsonNode payload) {
}
