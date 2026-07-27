package com.aliozcan.airportops.iam_service.tenant.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InviteOrganizationMemberRequest(
        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(max = 150)
        String fullName,

        @NotBlank
        @Pattern(regexp = "OPS_USER|VIEWER")
        String intendedRole
) {

    public InviteOrganizationMemberRequest {
        email = email == null ? null : email.trim();
        fullName = fullName == null ? null : fullName.trim();
    }
}
