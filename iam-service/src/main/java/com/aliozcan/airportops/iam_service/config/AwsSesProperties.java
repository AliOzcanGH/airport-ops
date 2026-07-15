package com.aliozcan.airportops.iam_service.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.aws")
public record AwsSesProperties(
        @NotBlank String region
) {
}
