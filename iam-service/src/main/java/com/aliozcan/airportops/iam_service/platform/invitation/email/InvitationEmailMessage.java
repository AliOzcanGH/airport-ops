package com.aliozcan.airportops.iam_service.platform.invitation.email;

import java.time.Instant;

public record InvitationEmailMessage(
        String recipientEmail,
        String organizationName,
        String acceptUrl,
        Instant expiresAt
) {
}
