package com.aliozcan.airportops.report_service.event;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record StationEventEnvelope(
        UUID eventId,
        String eventType,
        UUID organizationId,
        String occurredAt,
        JsonNode payload) {
}
