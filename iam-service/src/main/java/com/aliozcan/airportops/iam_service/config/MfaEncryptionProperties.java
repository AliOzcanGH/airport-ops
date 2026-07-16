package com.aliozcan.airportops.iam_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mfa")
public record MfaEncryptionProperties(
        String encryptionKey
) {
}
