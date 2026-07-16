package com.aliozcan.airportops.iam_service.domain.model;

import com.aliozcan.airportops.iam_service.domain.model.enums.TotpCredentialStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_totp_credentials", schema = "iam")
public class UserTotpCredentialEntity {

    public static final String CURRENT_KEY_VERSION = "v1";

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TotpCredentialStatus status;

    @Column(name = "secret_ciphertext", nullable = false, columnDefinition = "TEXT")
    private String secretCiphertext;

    @Column(name = "secret_nonce", nullable = false, length = 64)
    private String secretNonce;

    @Column(name = "secret_key_version", nullable = false, length = 30)
    private String secretKeyVersion;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserTotpCredentialEntity() {
    }

    private UserTotpCredentialEntity(
            UUID id,
            UUID userId,
            TotpCredentialStatus status,
            String secretCiphertext,
            String secretNonce,
            String secretKeyVersion,
            Instant verifiedAt,
            Instant disabledAt,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.secretCiphertext = secretCiphertext;
        this.secretNonce = secretNonce;
        this.secretKeyVersion = secretKeyVersion;
        this.verifiedAt = verifiedAt;
        this.disabledAt = disabledAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserTotpCredentialEntity enabled(
            UUID userId,
            String secretCiphertext,
            String secretNonce,
            Instant now) {
        return new UserTotpCredentialEntity(
                UUID.randomUUID(),
                userId,
                TotpCredentialStatus.ENABLED,
                secretCiphertext,
                secretNonce,
                CURRENT_KEY_VERSION,
                now,
                null,
                now,
                now
        );
    }

    public void enableWithSecret(
            String newSecretCiphertext,
            String newSecretNonce,
            Instant now) {
        this.status = TotpCredentialStatus.ENABLED;
        this.secretCiphertext = newSecretCiphertext;
        this.secretNonce = newSecretNonce;
        this.secretKeyVersion = CURRENT_KEY_VERSION;
        this.verifiedAt = now;
        this.disabledAt = null;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public TotpCredentialStatus getStatus() {
        return status;
    }

    public String getSecretCiphertext() {
        return secretCiphertext;
    }

    public String getSecretNonce() {
        return secretNonce;
    }

    public String getSecretKeyVersion() {
        return secretKeyVersion;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getDisabledAt() {
        return disabledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
