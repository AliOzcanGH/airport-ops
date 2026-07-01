package com.aliozcan.airportops.iam_service.platform.invitation.dto;

import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record PlatformInvitationResponse(
        UUID id,
        String email,
        String organizationName,
        InvitationStatus status,
        Instant expiresAt,
        String invitationToken
) {
}
