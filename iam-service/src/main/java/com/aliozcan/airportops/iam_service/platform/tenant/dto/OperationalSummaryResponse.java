package com.aliozcan.airportops.iam_service.platform.tenant.dto;

import java.time.Instant;
import java.util.UUID;

public record OperationalSummaryResponse(
        UUID organizationId,
        int stationCount,
        int totalFlightsLast30Days,
        Instant lastFlightActivityAt
) {
}
