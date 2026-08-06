package com.aliozcan.airportops.flight_service.task.dto;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID flightId,
        String taskType,
        String status,
        String assignedTo,
        Instant dueAt,
        Instant createdAt,
        Instant updatedAt
) {
}
