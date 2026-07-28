package com.aliozcan.airportops.iam_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.iam-token")
public record IamTokenProperties(
        String privateKey,
        String keyId,
        long ttlSeconds
) {
}
