package com.aliozcan.airportops.flight_service.event;

import java.time.Instant;
import java.util.UUID;

public record FlightCreatedEvent(
        UUID flightId,
        UUID organizationId,
        String flightNumber,
        String origin,
        String destination,
        Instant scheduledDeparture,
        Instant scheduledArrival,
        UUID assignedGateId,
        String status) {
}
