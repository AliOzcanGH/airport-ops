package com.aliozcan.airportops.iam_service.platform.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9_-]{43}")
        String token,

        @NotBlank
        @Size(max = 150)
        String fullName,

        @NotBlank
        @Size(min = 12, max = 128)
        String password,

        @NotBlank
        @Pattern(regexp = "TR|EN")
        String preferredLanguage
) {

    public AcceptInvitationRequest {
        fullName = fullName == null ? null : fullName.trim();
    }
}
