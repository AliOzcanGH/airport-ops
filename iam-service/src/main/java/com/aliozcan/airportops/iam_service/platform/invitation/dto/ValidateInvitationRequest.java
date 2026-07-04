package com.aliozcan.airportops.iam_service.platform.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ValidateInvitationRequest(
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9_-]{43}")
        String token
) {
}
