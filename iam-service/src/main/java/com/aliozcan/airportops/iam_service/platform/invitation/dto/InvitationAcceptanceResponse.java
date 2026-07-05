package com.aliozcan.airportops.iam_service.platform.invitation.dto;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;
import com.aliozcan.airportops.iam_service.domain.model.enums.UserStatus;

public record InvitationAcceptanceResponse(
        String email,
        String organizationName,
        OrganizationStatus organizationStatus,
        UserStatus userStatus,
        ProvisioningStatus provisioningStatus,
        String message
) {
}
