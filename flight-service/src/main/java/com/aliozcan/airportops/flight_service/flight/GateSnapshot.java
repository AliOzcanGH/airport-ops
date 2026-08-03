package com.aliozcan.airportops.flight_service.flight;

import java.util.UUID;

public record GateSnapshot(
        UUID id,
        UUID stationId,
        String code,
        String status
) {
}
