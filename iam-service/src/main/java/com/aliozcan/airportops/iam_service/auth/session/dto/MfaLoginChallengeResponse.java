package com.aliozcan.airportops.iam_service.auth.session.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MfaLoginChallengeResponse(
        String outcome,
        UUID challengeId,
        Instant expiresAt,
        int attemptsRemaining,
        String otpauthUri,
        String manualEntryKey
) {
    public static MfaLoginChallengeResponse verificationRequired(
            UUID challengeId,
            Instant expiresAt,
            int attemptsRemaining) {
        return new MfaLoginChallengeResponse(
                "MFA_REQUIRED",
                challengeId,
                expiresAt,
                attemptsRemaining,
                null,
                null);
    }

    public static MfaLoginChallengeResponse enrollmentRequired(
            UUID challengeId,
            Instant expiresAt,
            int attemptsRemaining,
            String otpauthUri,
            String manualEntryKey) {
        return new MfaLoginChallengeResponse(
                "MFA_ENROLLMENT_REQUIRED",
                challengeId,
                expiresAt,
                attemptsRemaining,
                otpauthUri,
                manualEntryKey);
    }
}
