package com.aliozcan.airportops.iam_service.auth.session.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SessionLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
    public SessionLoginRequest {
        email = email == null ? null : email.trim();
    }
}
