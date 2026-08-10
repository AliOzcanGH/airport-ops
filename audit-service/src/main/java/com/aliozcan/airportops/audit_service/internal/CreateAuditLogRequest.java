package com.aliozcan.airportops.audit_service.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CreateAuditLogRequest(
        UUID organizationId,
        UUID actorUserId,
        String actorEmail,
        @NotBlank String action,
        @NotBlank String resourceType,
        UUID resourceId,
        @NotNull Instant occurredAt,
        Map<String, Object> metadata) {
}
