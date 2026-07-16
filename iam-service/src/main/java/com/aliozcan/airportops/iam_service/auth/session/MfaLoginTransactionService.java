package com.aliozcan.airportops.iam_service.auth.session;

import com.aliozcan.airportops.iam_service.auth.UserNotProvisionedException;
import com.aliozcan.airportops.iam_service.auth.mfa.EncryptedValue;
import com.aliozcan.airportops.iam_service.auth.mfa.TotpSecretEncryptionService;
import com.aliozcan.airportops.iam_service.auth.mfa.TotpService;
import com.aliozcan.airportops.iam_service.auth.session.dto.MfaLoginChallengeResponse;
import com.aliozcan.airportops.iam_service.domain.model.MfaLoginChallengeEntity;
import com.aliozcan.airportops.iam_service.domain.model.UserTotpCredentialEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.MfaChallengeStatus;
import com.aliozcan.airportops.iam_service.domain.model.enums.MfaChallengeType;
import com.aliozcan.airportops.iam_service.domain.model.enums.TotpCredentialStatus;
import com.aliozcan.airportops.iam_service.repository.MfaLoginChallengeRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.repository.UserTotpCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class MfaLoginTransactionService {

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final UserTotpCredentialRepository credentialRepository;
    private final MfaLoginChallengeRepository challengeRepository;
    private final TotpSecretEncryptionService encryptionService;
    private final TotpService totpService;

    public MfaLoginTransactionService(
            UserRepository userRepository,
            UserTotpCredentialRepository credentialRepository,
            MfaLoginChallengeRepository challengeRepository,
            TotpSecretEncryptionService encryptionService,
            TotpService totpService) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.challengeRepository = challengeRepository;
        this.encryptionService = encryptionService;
        this.totpService = totpService;
    }

    @Transactional
    public MfaLoginChallengeResponse createChallenge(
            String email,
            KeycloakTokenResponse tokens,
            Instant tokenObtainedAt) {
        UUID userId = userRepository.findActiveByEmail(email)
                .orElseThrow(UserNotProvisionedException::new)
                .getId();
        int accessExpiresIn = tokenLifetime(tokens.expiresIn());
        int refreshExpiresIn = tokenLifetime(tokens.refreshExpiresIn());
        Instant accessExpiresAt = plusSeconds(tokenObtainedAt, accessExpiresIn);
        Instant expiresAt = earliest(
                tokenObtainedAt.plus(CHALLENGE_TTL),
                accessExpiresAt);
        EncryptedValue encryptedAccessToken = encrypt(tokens.accessToken());
        EncryptedValue encryptedRefreshToken = encrypt(tokens.refreshToken());

        if (credentialRepository.findEnabledByUserId(userId).isPresent()) {
            MfaLoginChallengeEntity challenge = challengeRepository.save(
                    MfaLoginChallengeEntity.pendingVerify(
                            userId,
                            encryptedAccessToken.ciphertext(),
                            encryptedAccessToken.nonce(),
                            encryptedRefreshToken.ciphertext(),
                            encryptedRefreshToken.nonce(),
                            tokenObtainedAt,
                            accessExpiresIn,
                            refreshExpiresIn,
                            expiresAt,
                            tokenObtainedAt));
            return MfaLoginChallengeResponse.verificationRequired(
                    challenge.getId(),
                    challenge.getExpiresAt(),
                    challenge.getMaxAttempts() - challenge.getAttemptCount());
        }

        String secret = totpService.generateSecret();
        EncryptedValue encryptedSecret = encrypt(secret);
        MfaLoginChallengeEntity challenge = challengeRepository.save(
                MfaLoginChallengeEntity.pendingEnroll(
                        userId,
                        encryptedAccessToken.ciphertext(),
                        encryptedAccessToken.nonce(),
                        encryptedRefreshToken.ciphertext(),
                        encryptedRefreshToken.nonce(),
                        tokenObtainedAt,
                        accessExpiresIn,
                        refreshExpiresIn,
                        encryptedSecret.ciphertext(),
                        encryptedSecret.nonce(),
                        expiresAt,
                        tokenObtainedAt));
        return MfaLoginChallengeResponse.enrollmentRequired(
                challenge.getId(),
                challenge.getExpiresAt(),
                challenge.getMaxAttempts() - challenge.getAttemptCount(),
                totpService.otpauthUri(email, secret),
                secret);
    }

    @Transactional
    public MfaVerificationResult verify(
            UUID challengeId,
            String code,
            Instant now) {
        MfaLoginChallengeEntity challenge = challengeRepository
                .findByIdForUpdate(challengeId)
                .orElse(null);
        if (challenge == null) {
            return MfaVerificationResult.failed(
                    MfaVerificationResult.Failure.EXPIRED);
        }
        if (challenge.getStatus() == MfaChallengeStatus.LOCKED) {
            return MfaVerificationResult.failed(
                    MfaVerificationResult.Failure.LOCKED);
        }
        if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
            challenge.lock(now);
            return MfaVerificationResult.failed(
                    MfaVerificationResult.Failure.LOCKED);
        }
        if (challenge.getStatus() != MfaChallengeStatus.PENDING
                || !challenge.getExpiresAt().isAfter(now)) {
            challenge.expire(now);
            return MfaVerificationResult.failed(
                    MfaVerificationResult.Failure.EXPIRED);
        }
        if (userRepository.findByIdForUpdate(challenge.getUserId()).isEmpty()) {
            challenge.expire(now);
            return MfaVerificationResult.failed(
                    MfaVerificationResult.Failure.EXPIRED);
        }

        String secret = verificationSecret(challenge);
        if (secret == null) {
            challenge.expire(now);
            return MfaVerificationResult.failed(
                    MfaVerificationResult.Failure.EXPIRED);
        }
        if (!verifyCode(secret, code)) {
            challenge.recordFailedAttempt(now);
            if (challenge.getStatus() == MfaChallengeStatus.LOCKED) {
                return MfaVerificationResult.failed(
                        MfaVerificationResult.Failure.LOCKED);
            }
            return MfaVerificationResult.failed(
                    MfaVerificationResult.Failure.INVALID_CODE);
        }

        KeycloakTokenResponse remainingTokens = remainingTokens(challenge, now);
        if (remainingTokens == null) {
            challenge.expire(now);
            return MfaVerificationResult.failed(
                    MfaVerificationResult.Failure.EXPIRED);
        }

        if (challenge.getType() == MfaChallengeType.ENROLL) {
            enableEnrollmentCredential(challenge.getUserId(), secret, now);
        }

        challengeRepository.delete(challenge);
        challengeRepository.flush();
        return MfaVerificationResult.success(remainingTokens);
    }

    private String verificationSecret(MfaLoginChallengeEntity challenge) {
        if (challenge.getType() == MfaChallengeType.ENROLL) {
            if (challenge.getTotpSecretCiphertext() == null
                    || challenge.getTotpSecretNonce() == null) {
                return null;
            }
            return decrypt(
                    challenge.getTotpSecretCiphertext(),
                    challenge.getTotpSecretNonce());
        }
        return credentialRepository.findEnabledByUserId(challenge.getUserId())
                .map(credential -> decrypt(
                        credential.getSecretCiphertext(),
                        credential.getSecretNonce()))
                .orElse(null);
    }

    private void enableEnrollmentCredential(UUID userId, String secret, Instant now) {
        UserTotpCredentialEntity credential = credentialRepository
                .findByUserId(userId)
                .orElse(null);
        if (credential != null && credential.getStatus() == TotpCredentialStatus.ENABLED) {
            return;
        }

        EncryptedValue encryptedSecret = encrypt(secret);
        if (credential == null) {
            credentialRepository.save(UserTotpCredentialEntity.enabled(
                    userId,
                    encryptedSecret.ciphertext(),
                    encryptedSecret.nonce(),
                    now));
        } else {
            credential.enableWithSecret(
                    encryptedSecret.ciphertext(),
                    encryptedSecret.nonce(),
                    now);
        }
    }

    private KeycloakTokenResponse remainingTokens(
            MfaLoginChallengeEntity challenge,
            Instant now) {
        long accessRemaining = remainingLifetime(
                challenge.getTokenObtainedAt(),
                challenge.getAccessExpiresIn(),
                now);
        if (accessRemaining <= 0) {
            return null;
        }
        long refreshRemaining = Math.max(0, remainingLifetime(
                challenge.getTokenObtainedAt(),
                challenge.getRefreshExpiresIn(),
                now));
        return new KeycloakTokenResponse(
                decrypt(
                        challenge.getAccessTokenCiphertext(),
                        challenge.getAccessTokenNonce()),
                decrypt(
                        challenge.getRefreshTokenCiphertext(),
                        challenge.getRefreshTokenNonce()),
                accessRemaining,
                refreshRemaining);
    }

    private long remainingLifetime(Instant obtainedAt, int expiresIn, Instant now) {
        return Duration.between(now, plusSeconds(obtainedAt, expiresIn)).getSeconds();
    }

    private int tokenLifetime(long lifetime) {
        try {
            return Math.toIntExact(lifetime);
        } catch (ArithmeticException exception) {
            throw new MfaConfigurationException(exception);
        }
    }

    private Instant plusSeconds(Instant instant, long seconds) {
        try {
            return instant.plusSeconds(seconds);
        } catch (DateTimeException | ArithmeticException exception) {
            throw new MfaConfigurationException(exception);
        }
    }

    private Instant earliest(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private EncryptedValue encrypt(String plaintext) {
        try {
            return encryptionService.encrypt(plaintext);
        } catch (RuntimeException exception) {
            throw new MfaConfigurationException(exception);
        }
    }

    private String decrypt(String ciphertext, String nonce) {
        try {
            return encryptionService.decrypt(new EncryptedValue(ciphertext, nonce));
        } catch (RuntimeException exception) {
            throw new MfaConfigurationException(exception);
        }
    }

    private boolean verifyCode(String secret, String code) {
        try {
            return totpService.verify(secret, code);
        } catch (RuntimeException exception) {
            throw new MfaConfigurationException(exception);
        }
    }
}
