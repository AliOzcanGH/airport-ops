package com.aliozcan.airportops.airport_service.gate.dto;

import java.time.Instant;
import java.util.UUID;

public record GateResponse(
        UUID id,
        UUID stationId,
        String code,
        String terminal,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
