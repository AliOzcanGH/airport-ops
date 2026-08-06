package com.aliozcan.airportops.iam_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.audit-service")
public record AuditServiceProperties(String baseUrl) {
}
