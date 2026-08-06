package com.aliozcan.airportops.audit_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", schema = "audit")
public class AuditLogEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_email")
    private String actorEmail;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLogEntity() {
    }

    private AuditLogEntity(
            UUID id,
            UUID organizationId,
            UUID actorUserId,
            String actorEmail,
            String action,
            String resourceType,
            UUID resourceId,
            Instant occurredAt,
            String metadata,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.actorUserId = actorUserId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.occurredAt = occurredAt;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public static AuditLogEntity record(
            UUID organizationId,
            UUID actorUserId,
            String actorEmail,
            String action,
            String resourceType,
            UUID resourceId,
            Instant occurredAt,
            String metadata) {
        return new AuditLogEntity(
                UUID.randomUUID(),
                organizationId,
                actorUserId,
                actorEmail,
                action,
                resourceType,
                resourceId,
                occurredAt,
                metadata,
                Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
