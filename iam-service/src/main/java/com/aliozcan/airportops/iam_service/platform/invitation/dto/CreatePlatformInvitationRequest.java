package com.aliozcan.airportops.iam_service.platform.invitation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePlatformInvitationRequest(
        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(max = 200)
        String organizationName
) {

    public CreatePlatformInvitationRequest {
        email = email == null ? null : email.trim();
        organizationName = organizationName == null
                ? null
                : organizationName.trim();
    }
}
