package com.aliozcan.airportops.iam_service.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.session.cookie")
public record SessionCookieProperties(
        @NotBlank String accessName,
        @NotBlank String refreshName,
        @NotBlank String sameSite,
        boolean secure
) {
}
