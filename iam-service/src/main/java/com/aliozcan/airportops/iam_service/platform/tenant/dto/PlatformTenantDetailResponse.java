package com.aliozcan.airportops.iam_service.platform.tenant.dto;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlatformTenantDetailResponse(
        UUID organizationId,
        String organizationName,
        OrganizationStatus organizationStatus,
        Instant createdAt,
        long memberCount,
        String primaryAdminEmail,
        List<PlatformTenantMemberResponse> members
) {
}
