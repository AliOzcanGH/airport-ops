package com.aliozcan.airportops.iam_service.auth.dto;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;

import java.util.SortedSet;
import java.util.UUID;

public record TenantContextResponse(
        UUID organizationId,
        String organizationName,
        OrganizationStatus organizationStatus,
        SortedSet<String> roles,
        SortedSet<String> permissions
) {
}
