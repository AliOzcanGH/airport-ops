package com.aliozcan.airportops.iam_service.domain.model;

import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationEmailDeliveryStatus;
import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationStatus;
import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationType;
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

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_delivery_status", nullable = false, length = 20)
    private InvitationEmailDeliveryStatus emailDeliveryStatus;

    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    @Column(name = "email_failure_reason", length = 500)
    private String emailFailureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_type", nullable = false, length = 30)
    private InvitationType invitationType;

    @Column(name = "intended_role", length = 30)
    private String intendedRole;

    @Column(name = "invitee_full_name", length = 150)
    private String inviteeFullName;

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
            Instant updatedAt,
            UUID organizationId,
            Instant acceptedAt,
            InvitationEmailDeliveryStatus emailDeliveryStatus,
            Instant emailSentAt,
            String emailFailureReason,
            InvitationType invitationType,
            String intendedRole,
            String inviteeFullName) {
        this.id = id;
        this.companyName = companyName;
        this.adminEmail = adminEmail;
        this.tokenHash = tokenHash;
        this.status = status;
        this.createdByUserId = createdByUserId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.organizationId = organizationId;
        this.acceptedAt = acceptedAt;
        this.emailDeliveryStatus = emailDeliveryStatus;
        this.emailSentAt = emailSentAt;
        this.emailFailureReason = emailFailureReason;
        this.invitationType = invitationType;
        this.intendedRole = intendedRole;
        this.inviteeFullName = inviteeFullName;
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
                createdAt,
                null,
                null,
                InvitationEmailDeliveryStatus.NOT_SENT,
                null,
                null,
                InvitationType.PLATFORM,
                null,
                null
        );
    }

    public static InvitationEntity pendingForOrganization(
            UUID organizationId,
            String organizationName,
            String adminEmail,
            String inviteeFullName,
            String intendedRole,
            String tokenHash,
            UUID createdByUserId,
            Instant createdAt,
            Instant expiresAt) {
        return new InvitationEntity(
                UUID.randomUUID(),
                organizationName,
                adminEmail,
                tokenHash,
                InvitationStatus.PENDING,
                createdByUserId,
                expiresAt,
                createdAt,
                createdAt,
                organizationId,
                null,
                InvitationEmailDeliveryStatus.NOT_SENT,
                null,
                null,
                InvitationType.ORGANIZATION,
                intendedRole,
                inviteeFullName
        );
    }

    public void accept(UUID organizationId, Instant now) {
        if (status != InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation is not pending");
        }
        this.status = InvitationStatus.ACCEPTED;
        this.organizationId = organizationId;
        this.acceptedAt = now;
        this.updatedAt = now;
    }

    public void markEmailSent(Instant now) {
        this.emailDeliveryStatus = InvitationEmailDeliveryStatus.SENT;
        this.emailSentAt = now;
        this.emailFailureReason = null;
        this.updatedAt = now;
    }

    public void markEmailFailed(String sanitizedReason, Instant now) {
        this.emailDeliveryStatus = InvitationEmailDeliveryStatus.FAILED;
        this.emailSentAt = null;
        this.emailFailureReason = sanitizedReason;
        this.updatedAt = now;
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

    public UUID getOrganizationId() {
        return organizationId;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public InvitationEmailDeliveryStatus getEmailDeliveryStatus() {
        return emailDeliveryStatus;
    }

    public Instant getEmailSentAt() {
        return emailSentAt;
    }

    public String getEmailFailureReason() {
        return emailFailureReason;
    }

    public InvitationType getInvitationType() {
        return invitationType;
    }

    public String getIntendedRole() {
        return intendedRole;
    }

    public String getInviteeFullName() {
        return inviteeFullName;
    }
}
