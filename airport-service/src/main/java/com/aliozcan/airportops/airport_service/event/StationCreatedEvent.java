package com.aliozcan.airportops.airport_service.event;

import java.util.UUID;

public record StationCreatedEvent(
        UUID stationId,
        UUID organizationId,
        String stationName,
        int gateCount) {
}
