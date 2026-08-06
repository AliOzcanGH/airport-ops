package com.aliozcan.airportops.iam_service.tenant.member.dto;

import java.util.UUID;

public record MemberRoleUpdateResponse(
        UUID memberId,
        String role
) {
}
