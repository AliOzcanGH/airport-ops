package com.aliozcan.airportops.iam_service.tenant.member.dto;

import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationEmailDeliveryStatus;
import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record OrganizationMemberInvitationResponse(
        UUID id,
        String email,
        String fullName,
        String intendedRole,
        InvitationStatus status,
        Instant expiresAt,
        InvitationEmailDeliveryStatus emailDeliveryStatus,
        Instant emailSentAt,
        String devAcceptLink
) {
}
