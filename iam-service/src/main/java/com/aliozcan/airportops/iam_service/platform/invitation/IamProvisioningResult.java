package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;

import java.util.UUID;

record IamProvisioningResult(
        UUID userId,
        String email,
        String organizationName,
        OrganizationStatus organizationStatus
) {
}
