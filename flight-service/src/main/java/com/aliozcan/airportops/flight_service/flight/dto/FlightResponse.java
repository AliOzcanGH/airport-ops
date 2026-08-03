package com.aliozcan.airportops.flight_service.flight.dto;

import java.time.Instant;
import java.util.UUID;

public record FlightResponse(
        UUID id,
        UUID organizationId,
        String flightNumber,
        String origin,
        String destination,
        Instant scheduledDeparture,
        Instant scheduledArrival,
        String status,
        UUID assignedGateId,
        Instant createdAt,
        Instant updatedAt
) {
}
