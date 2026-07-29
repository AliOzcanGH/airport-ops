package com.aliozcan.airportops.airport_service.station.dto;

import java.time.Instant;
import java.util.UUID;

public record StationResponse(
        UUID id,
        UUID organizationId,
        String stationName,
        String airportCode,
        int gateCount,
        Instant createdAt
) {
}
