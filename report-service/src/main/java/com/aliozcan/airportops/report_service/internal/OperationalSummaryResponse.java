package com.aliozcan.airportops.report_service.internal;

import com.aliozcan.airportops.report_service.readmodel.OrganizationOperationalSummaryEntity;

import java.time.Instant;
import java.util.UUID;

public record OperationalSummaryResponse(
        UUID organizationId,
        int stationCount,
        int totalFlightsLast30Days,
        Instant lastFlightActivityAt) {

    public static OperationalSummaryResponse from(UUID organizationId, OrganizationOperationalSummaryEntity entity) {
        if (entity == null) {
            return new OperationalSummaryResponse(organizationId, 0, 0, null);
        }
        return new OperationalSummaryResponse(
                organizationId, entity.getStationCount(), entity.getTotalFlightsLast30Days(),
                entity.getLastFlightActivityAt());
    }
}
