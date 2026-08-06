package com.aliozcan.airportops.flight_service.event;

import java.time.Instant;
import java.util.UUID;

public record TaskCompletedEvent(
        UUID taskId,
        UUID flightId,
        UUID organizationId,
        String taskType,
        Instant completedAt) {
}
