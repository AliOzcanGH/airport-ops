package com.aliozcan.airportops.iam_service.platform.tenant.dto;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;

import java.time.Instant;
import java.util.UUID;

public record PlatformTenantSummaryResponse(
        UUID organizationId,
        String organizationName,
        OrganizationStatus organizationStatus,
        Instant createdAt,
        long memberCount,
        String primaryAdminEmail
) {
}
