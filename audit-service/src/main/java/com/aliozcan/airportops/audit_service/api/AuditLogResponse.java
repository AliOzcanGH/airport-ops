package com.aliozcan.airportops.audit_service.api;

import com.aliozcan.airportops.audit_service.domain.AuditLogEntity;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID organizationId,
        UUID actorUserId,
        String actorEmail,
        String action,
        String resourceType,
        UUID resourceId,
        Instant occurredAt,
        String metadata
) {
    public static AuditLogResponse from(AuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getActorUserId(),
                entity.getActorEmail(),
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getOccurredAt(),
                entity.getMetadata());
    }
}
