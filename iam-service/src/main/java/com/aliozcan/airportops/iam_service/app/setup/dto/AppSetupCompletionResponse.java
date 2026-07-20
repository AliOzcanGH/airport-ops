package com.aliozcan.airportops.iam_service.app.setup.dto;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;

import java.time.Instant;
import java.util.UUID;

public record AppSetupCompletionResponse(
        UUID organizationId,
        OrganizationStatus organizationStatus,
        Instant completedAt
) {
}
