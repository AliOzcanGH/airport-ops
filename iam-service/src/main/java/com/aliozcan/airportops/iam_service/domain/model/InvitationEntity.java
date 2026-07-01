package com.aliozcan.airportops.iam_service.domain.model;

import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitations", schema = "iam")
public class InvitationEntity {

    @Id
    private UUID id;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "admin_email", nullable = false, length = 320)
    private String adminEmail;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvitationStatus status;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InvitationEntity() {
    }

    private InvitationEntity(
            UUID id,
            String companyName,
            String adminEmail,
            String tokenHash,
            InvitationStatus status,
            UUID createdByUserId,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.companyName = companyName;
        this.adminEmail = adminEmail;
        this.tokenHash = tokenHash;
        this.status = status;
        this.createdByUserId = createdByUserId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static InvitationEntity pending(
            String companyName,
            String adminEmail,
            String tokenHash,
            UUID createdByUserId,
            Instant createdAt,
            Instant expiresAt) {
        return new InvitationEntity(
                UUID.randomUUID(),
                companyName,
                adminEmail,
                tokenHash,
                InvitationStatus.PENDING,
                createdByUserId,
                expiresAt,
                createdAt,
                createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
