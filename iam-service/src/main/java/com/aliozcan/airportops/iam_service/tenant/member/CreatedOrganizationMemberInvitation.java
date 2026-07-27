package com.aliozcan.airportops.iam_service.tenant.member;

import com.aliozcan.airportops.iam_service.domain.model.InvitationEntity;

public record CreatedOrganizationMemberInvitation(
        InvitationEntity invitation,
        String rawToken
) {
}
