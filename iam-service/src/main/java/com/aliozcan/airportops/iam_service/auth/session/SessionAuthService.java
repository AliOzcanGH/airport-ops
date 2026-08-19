package com.aliozcan.airportops.iam_service.auth.session;

import com.aliozcan.airportops.iam_service.auth.InvalidLoginException;
import com.aliozcan.airportops.iam_service.auth.LoginAttemptGuard;
import com.aliozcan.airportops.iam_service.auth.session.dto.MfaLoginChallengeResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
public class SessionAuthService {

    private final KeycloakSessionClient keycloakSessionClient;
    private final SessionCookieService cookieService;
    private final MfaLoginTransactionService mfaLoginTransactionService;
    private final LoginAttemptGuard loginAttemptGuard;
    private final Clock clock;

    public SessionAuthService(
            KeycloakSessionClient keycloakSessionClient,
            SessionCookieService cookieService,
            MfaLoginTransactionService mfaLoginTransactionService,
            LoginAttemptGuard loginAttemptGuard,
            Clock clock) {
        this.keycloakSessionClient = keycloakSessionClient;
        this.cookieService = cookieService;
        this.mfaLoginTransactionService = mfaLoginTransactionService;
        this.loginAttemptGuard = loginAttemptGuard;
        this.clock = clock;
    }

    public MfaLoginChallengeResponse login(String email, String password) {
        String normalizedEmail = email.trim();
        loginAttemptGuard.checkNotLocked(normalizedEmail);

        KeycloakTokenResponse tokens;
        try {
            tokens = keycloakSessionClient.login(normalizedEmail, password);
        } catch (InvalidLoginException exception) {
            loginAttemptGuard.recordFailure(normalizedEmail);
            throw exception;
        }
        loginAttemptGuard.recordSuccess(normalizedEmail);

        return mfaLoginTransactionService.createChallenge(
                normalizedEmail,
                tokens,
                clock.instant());
    }

    public void verifyMfa(
            UUID challengeId,
            String code,
            HttpServletResponse response) {
        MfaVerificationResult result = mfaLoginTransactionService.verify(
                challengeId,
                code,
                clock.instant());
        if (result.failure() != null) {
            throw switch (result.failure()) {
                case EXPIRED -> new MfaChallengeExpiredException();
                case LOCKED -> new MfaChallengeLockedException();
                case INVALID_CODE -> new MfaCodeInvalidException();
            };
        }

        // The transactional MFA work has committed before session cookies are written.
        cookieService.writeTokens(response, result.tokens());
    }

    public void refresh(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            cookieService.clearTokens(response);
            throw new SessionExpiredException();
        }
        try {
            cookieService.writeTokens(response, keycloakSessionClient.refresh(refreshToken));
        } catch (SessionExpiredException exception) {
            cookieService.clearTokens(response);
            throw exception;
        }
    }

    public void logout(String refreshToken, HttpServletResponse response) {
        try {
            if (refreshToken != null && !refreshToken.isBlank()) {
                keycloakSessionClient.logout(refreshToken);
            }
        } catch (AuthProviderUnavailableException | SessionExpiredException ignored) {
            // Local logout must succeed even when the provider session is unavailable.
        } finally {
            cookieService.clearTokens(response);
        }
    }
}
