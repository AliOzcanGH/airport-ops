package com.aliozcan.airportops.iam_service.tenant.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateMemberRoleRequest(
        @NotBlank
        @Pattern(regexp = "OPS_USER|VIEWER")
        String role
) {
}
