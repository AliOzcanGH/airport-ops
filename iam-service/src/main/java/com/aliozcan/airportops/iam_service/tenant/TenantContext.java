package com.aliozcan.airportops.iam_service.tenant;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;

import java.util.SortedSet;
import java.util.UUID;

public record TenantContext(
        UUID organizationId,
        String organizationName,
        OrganizationStatus organizationStatus,
        SortedSet<String> roles,
        SortedSet<String> permissions
) {
}
