package com.aliozcan.airportops.flight_service.event;

import java.util.UUID;

public record FlightStatusChangedEvent(
        UUID flightId,
        UUID organizationId,
        String flightNumber,
        String previousStatus,
        String newStatus) {
}
