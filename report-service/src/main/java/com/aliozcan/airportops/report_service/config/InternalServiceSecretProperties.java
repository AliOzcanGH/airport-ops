package com.aliozcan.airportops.report_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record InternalServiceSecretProperties(
        String internalServiceSecret
) {
}
