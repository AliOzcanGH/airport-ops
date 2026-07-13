package com.aliozcan.airportops.iam_service.platform.tenant.dto;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationMemberStatus;

import java.time.Instant;
import java.util.SortedSet;
import java.util.UUID;

public record PlatformTenantMemberResponse(
        UUID memberId,
        UUID userId,
        String email,
        String fullName,
        OrganizationMemberStatus memberStatus,
        SortedSet<String> roles,
        Instant joinedAt
) {
}
