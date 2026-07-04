package com.aliozcan.airportops.iam_service.platform.invitation.dto;

import java.time.Instant;

public record InvitationValidationResponse(
        String organizationName,
        String invitedEmail,
        Instant expiresAt
) {
}
