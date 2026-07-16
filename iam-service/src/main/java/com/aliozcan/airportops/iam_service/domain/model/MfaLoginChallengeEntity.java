package com.aliozcan.airportops.iam_service.domain.model;

import com.aliozcan.airportops.iam_service.domain.model.enums.MfaChallengeStatus;
import com.aliozcan.airportops.iam_service.domain.model.enums.MfaChallengeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mfa_login_challenges", schema = "iam")
public class MfaLoginChallengeEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MfaChallengeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MfaChallengeStatus status;

    @Column(name = "access_token_ciphertext", nullable = false, columnDefinition = "TEXT")
    private String accessTokenCiphertext;

    @Column(name = "access_token_nonce", nullable = false, length = 64)
    private String accessTokenNonce;

    @Column(name = "refresh_token_ciphertext", nullable = false, columnDefinition = "TEXT")
    private String refreshTokenCiphertext;

    @Column(name = "refresh_token_nonce", nullable = false, length = 64)
    private String refreshTokenNonce;

    @Column(name = "token_obtained_at", nullable = false)
    private Instant tokenObtainedAt;

    @Column(name = "access_expires_in", nullable = false)
    private int accessExpiresIn;

    @Column(name = "refresh_expires_in", nullable = false)
    private int refreshExpiresIn;

    @Column(name = "totp_secret_ciphertext", columnDefinition = "TEXT")
    private String totpSecretCiphertext;

    @Column(name = "totp_secret_nonce", length = 64)
    private String totpSecretNonce;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MfaLoginChallengeEntity() {
    }

    private MfaLoginChallengeEntity(
            UUID id,
            UUID userId,
            MfaChallengeType type,
            MfaChallengeStatus status,
            String accessTokenCiphertext,
            String accessTokenNonce,
            String refreshTokenCiphertext,
            String refreshTokenNonce,
            Instant tokenObtainedAt,
            int accessExpiresIn,
            int refreshExpiresIn,
            String totpSecretCiphertext,
            String totpSecretNonce,
            int attemptCount,
            int maxAttempts,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.status = status;
        this.accessTokenCiphertext = accessTokenCiphertext;
        this.accessTokenNonce = accessTokenNonce;
        this.refreshTokenCiphertext = refreshTokenCiphertext;
        this.refreshTokenNonce = refreshTokenNonce;
        this.tokenObtainedAt = tokenObtainedAt;
        this.accessExpiresIn = accessExpiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
        this.totpSecretCiphertext = totpSecretCiphertext;
        this.totpSecretNonce = totpSecretNonce;
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MfaLoginChallengeEntity pendingVerify(
            UUID userId,
            String accessTokenCiphertext,
            String accessTokenNonce,
            String refreshTokenCiphertext,
            String refreshTokenNonce,
            Instant tokenObtainedAt,
            int accessExpiresIn,
            int refreshExpiresIn,
            Instant expiresAt,
            Instant now) {
        return pending(
                userId,
                MfaChallengeType.VERIFY,
                accessTokenCiphertext,
                accessTokenNonce,
                refreshTokenCiphertext,
                refreshTokenNonce,
                tokenObtainedAt,
                accessExpiresIn,
                refreshExpiresIn,
                null,
                null,
                expiresAt,
                now);
    }

    public static MfaLoginChallengeEntity pendingEnroll(
            UUID userId,
            String accessTokenCiphertext,
            String accessTokenNonce,
            String refreshTokenCiphertext,
            String refreshTokenNonce,
            Instant tokenObtainedAt,
            int accessExpiresIn,
            int refreshExpiresIn,
            String totpSecretCiphertext,
            String totpSecretNonce,
            Instant expiresAt,
            Instant now) {
        return pending(
                userId,
                MfaChallengeType.ENROLL,
                accessTokenCiphertext,
                accessTokenNonce,
                refreshTokenCiphertext,
                refreshTokenNonce,
                tokenObtainedAt,
                accessExpiresIn,
                refreshExpiresIn,
                totpSecretCiphertext,
                totpSecretNonce,
                expiresAt,
                now);
    }

    private static MfaLoginChallengeEntity pending(
            UUID userId,
            MfaChallengeType type,
            String accessTokenCiphertext,
            String accessTokenNonce,
            String refreshTokenCiphertext,
            String refreshTokenNonce,
            Instant tokenObtainedAt,
            int accessExpiresIn,
            int refreshExpiresIn,
            String totpSecretCiphertext,
            String totpSecretNonce,
            Instant expiresAt,
            Instant now) {
        return new MfaLoginChallengeEntity(
                UUID.randomUUID(),
                userId,
                type,
                MfaChallengeStatus.PENDING,
                accessTokenCiphertext,
                accessTokenNonce,
                refreshTokenCiphertext,
                refreshTokenNonce,
                tokenObtainedAt,
                accessExpiresIn,
                refreshExpiresIn,
                totpSecretCiphertext,
                totpSecretNonce,
                0,
                5,
                expiresAt,
                now,
                now
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public MfaChallengeType getType() {
        return type;
    }

    public MfaChallengeStatus getStatus() {
        return status;
    }

    public String getAccessTokenCiphertext() {
        return accessTokenCiphertext;
    }

    public String getAccessTokenNonce() {
        return accessTokenNonce;
    }

    public String getRefreshTokenCiphertext() {
        return refreshTokenCiphertext;
    }

    public String getRefreshTokenNonce() {
        return refreshTokenNonce;
    }

    public Instant getTokenObtainedAt() {
        return tokenObtainedAt;
    }

    public int getAccessExpiresIn() {
        return accessExpiresIn;
    }

    public int getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public String getTotpSecretCiphertext() {
        return totpSecretCiphertext;
    }

    public String getTotpSecretNonce() {
        return totpSecretNonce;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
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

    public void recordFailedAttempt(Instant now) {
        if (status != MfaChallengeStatus.PENDING) {
            return;
        }
        attemptCount++;
        if (attemptCount >= maxAttempts) {
            status = MfaChallengeStatus.LOCKED;
        }
        updatedAt = now;
    }

    public void expire(Instant now) {
        if (status == MfaChallengeStatus.PENDING) {
            status = MfaChallengeStatus.EXPIRED;
            updatedAt = now;
        }
    }

    public void lock(Instant now) {
        if (status == MfaChallengeStatus.PENDING) {
            status = MfaChallengeStatus.LOCKED;
            updatedAt = now;
        }
    }
}
