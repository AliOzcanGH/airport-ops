package com.aliozcan.airportops.report_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.iam")
public record IamJwtProperties(
        String jwksUri,
        String issuer,
        String audience
) {
}
