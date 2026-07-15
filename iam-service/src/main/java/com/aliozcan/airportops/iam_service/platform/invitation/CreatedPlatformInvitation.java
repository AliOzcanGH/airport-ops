package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.domain.model.InvitationEntity;

public record CreatedPlatformInvitation(
        InvitationEntity invitation,
        String rawToken
) {
}
