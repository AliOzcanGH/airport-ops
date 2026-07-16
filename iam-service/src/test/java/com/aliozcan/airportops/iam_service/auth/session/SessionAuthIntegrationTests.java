package com.aliozcan.airportops.iam_service.auth.session;

import com.aliozcan.airportops.iam_service.auth.InvalidLoginException;
import com.aliozcan.airportops.iam_service.auth.dto.AuthMeResponse;
import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.auth.mfa.EncryptedValue;
import com.aliozcan.airportops.iam_service.auth.mfa.TotpSecretEncryptionService;
import com.aliozcan.airportops.iam_service.auth.session.dto.CsrfMetadataResponse;
import com.aliozcan.airportops.iam_service.auth.session.dto.MfaLoginChallengeResponse;
import com.aliozcan.airportops.iam_service.auth.session.dto.MfaVerifyRequest;
import com.aliozcan.airportops.iam_service.auth.session.dto.SessionLoginRequest;
import com.aliozcan.airportops.iam_service.domain.model.UserTotpCredentialEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.MfaChallengeStatus;
import com.aliozcan.airportops.iam_service.domain.model.enums.TotpCredentialStatus;
import com.aliozcan.airportops.iam_service.repository.MfaLoginChallengeRepository;
import com.aliozcan.airportops.iam_service.repository.UserTotpCredentialRepository;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "DELETE FROM iam.mfa_login_challenges",
        "DELETE FROM iam.user_totp_credentials"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.mfa_login_challenges",
        "DELETE FROM iam.user_totp_credentials"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class SessionAuthIntegrationTests {

    private static final String EMAIL = "platform.admin@demo.com";
    private static final String PASSWORD = "Admin123!";
    private static final String ACCESS_COOKIE = "airport_ops_access_token";
    private static final String REFRESH_COOKIE = "airport_ops_refresh_token";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserTotpCredentialRepository credentialRepository;

    @Autowired
    private MfaLoginChallengeRepository challengeRepository;

    @Autowired
    private TotpSecretEncryptionService encryptionService;

    @Autowired
    private CodeGenerator codeGenerator;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private KeycloakSessionClient keycloakSessionClient;

    @BeforeEach
    void resetClient() {
        reset(keycloakSessionClient);
    }

    @Test
    void csrfEndpointCreatesReadableCookieAndReturnsMetadata() {
        ResponseEntity<CsrfMetadataResponse> response = csrf();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().headerName()).isEqualTo("X-XSRF-TOKEN");
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
                .anySatisfy(cookie -> assertThat(cookie)
                        .startsWith("XSRF-TOKEN=")
                        .contains("Path=/")
                        .doesNotContain("HttpOnly"));
    }

    @Test
    void passwordLoginWithoutEnabledCredentialRequiresEnrollmentAndWritesNoCookies() {
        stubLogin(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();

        ResponseEntity<MfaLoginChallengeResponse> response = login(csrf);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().outcome()).isEqualTo("MFA_ENROLLMENT_REQUIRED");
        assertThat(response.getBody().attemptsRemaining()).isEqualTo(5);
        assertThat(response.getBody().otpauthUri()).startsWith("otpauth://totp/");
        assertThat(response.getBody().manualEntryKey()).isNotBlank();
        assertThat(challengeRepository.findById(response.getBody().challengeId())).isPresent();
        assertNoSessionCookies(response);
        verify(keycloakSessionClient).login(EMAIL, PASSWORD);
    }

    @Test
    void passwordLoginWithEnabledCredentialRequiresVerificationAndWritesNoCookies() {
        enableCredential("JBSWY3DPEHPK3PXP");
        stubLogin(tokens("access-token", "refresh-token"));

        ResponseEntity<MfaLoginChallengeResponse> response = login(csrfContext());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().outcome()).isEqualTo("MFA_REQUIRED");
        assertThat(response.getBody().attemptsRemaining()).isEqualTo(5);
        assertThat(response.getBody().otpauthUri()).isNull();
        assertThat(response.getBody().manualEntryKey()).isNull();
        assertNoSessionCookies(response);
    }

    @Test
    void invalidLoginReturnsGenericUnauthorizedResponseWithoutCookies() {
        when(keycloakSessionClient.login(anyString(), anyString()))
                .thenThrow(new InvalidLoginException());

        ResponseEntity<ErrorResponse> response = loginError(csrfContext());

        assertError(response, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        assertNoSessionCookies(response);
        assertThat(challengeRepository.count()).isZero();
    }

    @Test
    void unavailableProviderReturnsServiceUnavailable() {
        when(keycloakSessionClient.login(anyString(), anyString()))
                .thenThrow(new AuthProviderUnavailableException(
                        new IllegalStateException("unavailable")));

        ResponseEntity<ErrorResponse> response = loginError(csrfContext());

        assertError(response, HttpStatus.SERVICE_UNAVAILABLE, "AUTH_PROVIDER_UNAVAILABLE");
        assertNoSessionCookies(response);
    }

    @Test
    void correctEnrollmentCodeEnablesCredentialWritesCookiesAndDeletesChallenge() {
        stubLogin(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();
        MfaLoginChallengeResponse challenge = login(csrf).getBody();
        assertThat(challenge).isNotNull();

        ResponseEntity<String> response = verifyMfa(
                csrf,
                challenge.challengeId(),
                currentCode(challenge.manualEntryKey()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertMfaTokenCookie(response, ACCESS_COOKIE, "access-token", 295, 300);
        assertMfaTokenCookie(response, REFRESH_COOKIE, "refresh-token", 1795, 1800);
        UserTotpCredentialEntity credential = credentialRepository
                .findEnabledByUserId(platformAdminId())
                .orElseThrow();
        assertThat(encryptionService.decrypt(new EncryptedValue(
                credential.getSecretCiphertext(),
                credential.getSecretNonce())))
                .isEqualTo(challenge.manualEntryKey());
        assertThat(challengeRepository.findById(challenge.challengeId())).isEmpty();
    }

    @Test
    void correctExistingTotpCodeWritesCookiesAndDeletesChallenge() {
        String secret = "JBSWY3DPEHPK3PXP";
        enableCredential(secret);
        stubLogin(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();
        MfaLoginChallengeResponse challenge = login(csrf).getBody();
        assertThat(challenge).isNotNull();

        ResponseEntity<String> response = verifyMfa(
                csrf,
                challenge.challengeId(),
                currentCode(secret),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertMfaTokenCookie(response, ACCESS_COOKIE, "access-token", 295, 300);
        assertMfaTokenCookie(response, REFRESH_COOKIE, "refresh-token", 1795, 1800);
        assertThat(challengeRepository.findById(challenge.challengeId())).isEmpty();
    }

    @Test
    void wrongCodeIncrementsAttemptsWithoutWritingCookies() {
        stubLogin(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();
        MfaLoginChallengeResponse challenge = login(csrf).getBody();
        assertThat(challenge).isNotNull();

        ResponseEntity<ErrorResponse> response = verifyMfa(
                csrf,
                challenge.challengeId(),
                differentCode(currentCode(challenge.manualEntryKey())),
                ErrorResponse.class);

        assertError(response, HttpStatus.UNAUTHORIZED, "MFA_CODE_INVALID");
        assertNoSessionCookies(response);
        assertThat(challengeRepository.findById(challenge.challengeId()))
                .get()
                .extracting(challengeEntity -> challengeEntity.getAttemptCount())
                .isEqualTo(1);
    }

    @Test
    void maxAttemptsLocksChallengeAndFurtherAttemptReturnsLocked() {
        stubLogin(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();
        MfaLoginChallengeResponse challenge = login(csrf).getBody();
        assertThat(challenge).isNotNull();
        String wrongCode = differentCode(currentCode(challenge.manualEntryKey()));

        for (int attempt = 0; attempt < 4; attempt++) {
            ResponseEntity<ErrorResponse> response = verifyMfa(
                    csrf,
                    challenge.challengeId(),
                    wrongCode,
                    ErrorResponse.class);
            assertError(response, HttpStatus.UNAUTHORIZED, "MFA_CODE_INVALID");
        }

        ResponseEntity<ErrorResponse> maxAttemptResponse = verifyMfa(
                csrf,
                challenge.challengeId(),
                wrongCode,
                ErrorResponse.class);
        assertError(
                maxAttemptResponse,
                HttpStatus.UNAUTHORIZED,
                "MFA_CHALLENGE_LOCKED");

        assertThat(challengeRepository.findById(challenge.challengeId()))
                .get()
                .satisfies(locked -> {
                    assertThat(locked.getAttemptCount()).isEqualTo(5);
                    assertThat(locked.getStatus()).isEqualTo(MfaChallengeStatus.LOCKED);
                });
        ResponseEntity<ErrorResponse> lockedResponse = verifyMfa(
                csrf,
                challenge.challengeId(),
                currentCode(challenge.manualEntryKey()),
                ErrorResponse.class);
        assertError(lockedResponse, HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_LOCKED");
        assertNoSessionCookies(lockedResponse);
    }

    @Test
    void alreadyVerifiedChallengeIsRejectedAsExpired() {
        stubLogin(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();
        MfaLoginChallengeResponse challenge = login(csrf).getBody();
        assertThat(challenge).isNotNull();
        jdbcTemplate.update(
                "UPDATE iam.mfa_login_challenges SET status = 'VERIFIED' WHERE id = ?",
                challenge.challengeId());

        ResponseEntity<ErrorResponse> response = verifyMfa(
                csrf,
                challenge.challengeId(),
                currentCode(challenge.manualEntryKey()),
                ErrorResponse.class);

        assertError(response, HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_EXPIRED");
        assertNoSessionCookies(response);
    }

    @Test
    void expiredChallengeIsRejectedWithoutCookies() {
        stubLogin(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();
        MfaLoginChallengeResponse challenge = login(csrf).getBody();
        assertThat(challenge).isNotNull();
        jdbcTemplate.update(
                "UPDATE iam.mfa_login_challenges SET expires_at = now() - interval '1 second' WHERE id = ?",
                challenge.challengeId());

        ResponseEntity<ErrorResponse> response = verifyMfa(
                csrf,
                challenge.challengeId(),
                currentCode(challenge.manualEntryKey()),
                ErrorResponse.class);

        assertError(response, HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_EXPIRED");
        assertNoSessionCookies(response);
    }

    @Test
    void successfulChallengeCannotBeReused() {
        stubLogin(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();
        MfaLoginChallengeResponse challenge = login(csrf).getBody();
        assertThat(challenge).isNotNull();
        String code = currentCode(challenge.manualEntryKey());

        ResponseEntity<String> first = verifyMfa(
                csrf,
                challenge.challengeId(),
                code,
                String.class);
        ResponseEntity<ErrorResponse> second = verifyMfa(
                csrf,
                challenge.challengeId(),
                code,
                ErrorResponse.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertError(second, HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_EXPIRED");
        assertNoSessionCookies(second);
    }

    @Test
    void disabledCredentialRowIsReusedAndUpdatedDuringEnrollment() {
        EncryptedValue disabledSecret = encryptionService.encrypt("JBSWY3DPEHPK3PXP");
        UUID credentialId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        INSERT INTO iam.user_totp_credentials (
                            id, user_id, status, secret_ciphertext, secret_nonce,
                            secret_key_version, disabled_at, created_at, updated_at)
                        VALUES (?, ?, 'DISABLED', ?, ?, 'v1', now(), now(), now())
                        """,
                credentialId,
                platformAdminId(),
                disabledSecret.ciphertext(),
                disabledSecret.nonce());
        stubLogin(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();
        MfaLoginChallengeResponse challenge = login(csrf).getBody();
        assertThat(challenge).isNotNull();

        ResponseEntity<String> response = verifyMfa(
                csrf,
                challenge.challengeId(),
                currentCode(challenge.manualEntryKey()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        UserTotpCredentialEntity credential = credentialRepository
                .findByUserId(platformAdminId())
                .orElseThrow();
        assertThat(credential.getId()).isEqualTo(credentialId);
        assertThat(credential.getStatus()).isEqualTo(TotpCredentialStatus.ENABLED);
        assertThat(encryptionService.decrypt(new EncryptedValue(
                credential.getSecretCiphertext(),
                credential.getSecretNonce())))
                .isEqualTo(challenge.manualEntryKey());
        assertThat(credentialRepository.count()).isEqualTo(1);
    }

    @Test
    void enrollmentDoesNotDuplicateOrReplaceCredentialEnabledAfterChallengeCreation() {
        stubLogin(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();
        MfaLoginChallengeResponse challenge = login(csrf).getBody();
        assertThat(challenge).isNotNull();
        String existingSecret = "JBSWY3DPEHPK3PXP";
        UserTotpCredentialEntity existingCredential = enableCredential(existingSecret);

        ResponseEntity<String> response = verifyMfa(
                csrf,
                challenge.challengeId(),
                currentCode(challenge.manualEntryKey()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        UserTotpCredentialEntity credential = credentialRepository
                .findByUserId(platformAdminId())
                .orElseThrow();
        assertThat(credential.getId()).isEqualTo(existingCredential.getId());
        assertThat(encryptionService.decrypt(new EncryptedValue(
                credential.getSecretCiphertext(),
                credential.getSecretNonce())))
                .isEqualTo(existingSecret);
        assertThat(credentialRepository.count()).isEqualTo(1);
    }

    @Test
    void cookieLifetimeIsRecomputedAfterMfaDelay() {
        stubLogin(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();
        MfaLoginChallengeResponse challenge = login(csrf).getBody();
        assertThat(challenge).isNotNull();
        jdbcTemplate.update(
                "UPDATE iam.mfa_login_challenges SET token_obtained_at = now() - interval '120 seconds' WHERE id = ?",
                challenge.challengeId());

        ResponseEntity<String> response = verifyMfa(
                csrf,
                challenge.challengeId(),
                currentCode(challenge.manualEntryKey()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertMfaTokenCookie(response, ACCESS_COOKIE, "access-token", 175, 180);
        assertMfaTokenCookie(response, REFRESH_COOKIE, "refresh-token", 1675, 1680);
    }

    @Test
    void expiredPendingAccessTokenDoesNotWriteCookies() {
        stubLogin(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();
        MfaLoginChallengeResponse challenge = login(csrf).getBody();
        assertThat(challenge).isNotNull();
        jdbcTemplate.update(
                """
                        UPDATE iam.mfa_login_challenges
                        SET token_obtained_at = now() - interval '301 seconds',
                            expires_at = now() + interval '60 seconds'
                        WHERE id = ?
                        """,
                challenge.challengeId());

        ResponseEntity<ErrorResponse> response = verifyMfa(
                csrf,
                challenge.challengeId(),
                currentCode(challenge.manualEntryKey()),
                ErrorResponse.class);

        assertError(response, HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_EXPIRED");
        assertNoSessionCookies(response);
        assertThat(credentialRepository.findByUserId(platformAdminId())).isEmpty();
    }

    @Test
    void missingChallengeIsRejectedAsExpired() {
        ResponseEntity<ErrorResponse> response = verifyMfa(
                csrfContext(),
                UUID.randomUUID(),
                "123456",
                ErrorResponse.class);

        assertError(response, HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_EXPIRED");
        assertNoSessionCookies(response);
    }

    @Test
    void rejectsMfaVerifyWithoutCsrf() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/auth/session/mfa/verify",
                new MfaVerifyRequest(UUID.randomUUID(), "123456"),
                ErrorResponse.class);

        assertError(response, HttpStatus.FORBIDDEN, "CSRF_VALIDATION_FAILED");
    }

    @Test
    void refreshRotatesBothCookies() {
        when(keycloakSessionClient.refresh("old-refresh"))
                .thenReturn(tokens("new-access", "new-refresh"));
        CsrfContext csrf = csrfContext();
        HttpHeaders headers = csrf.headers();
        headers.add(HttpHeaders.COOKIE, REFRESH_COOKIE + "=old-refresh");

        ResponseEntity<String> response = restTemplate.exchange(
                "/auth/session/refresh",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertTokenCookie(response, ACCESS_COOKIE, "new-access", 300);
        assertTokenCookie(response, REFRESH_COOKIE, "new-refresh", 1800);
    }

    @Test
    void invalidRefreshClearsBothCookies() {
        when(keycloakSessionClient.refresh("expired-refresh"))
                .thenThrow(new SessionExpiredException());
        CsrfContext csrf = csrfContext();
        HttpHeaders headers = csrf.headers();
        headers.add(HttpHeaders.COOKIE, REFRESH_COOKIE + "=expired-refresh");

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/auth/session/refresh",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                ErrorResponse.class);

        assertError(response, HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED");
        assertClearedCookie(response, ACCESS_COOKIE);
        assertClearedCookie(response, REFRESH_COOKIE);
    }

    @Test
    void logoutClearsCookiesEvenWhenProviderIsUnavailable() {
        doThrow(new AuthProviderUnavailableException(
                new IllegalStateException("unavailable")))
                .when(keycloakSessionClient).logout("refresh-token");
        CsrfContext csrf = csrfContext();
        HttpHeaders headers = csrf.headers();
        headers.add(HttpHeaders.COOKIE, REFRESH_COOKIE + "=refresh-token");

        ResponseEntity<String> response = restTemplate.exchange(
                "/auth/session/logout",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertClearedCookie(response, ACCESS_COOKIE);
        assertClearedCookie(response, REFRESH_COOKIE);
    }

    @Test
    void rejectsSessionLoginWithoutCsrf() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/auth/session/login",
                new SessionLoginRequest(EMAIL, PASSWORD),
                ErrorResponse.class);

        assertError(response, HttpStatus.FORBIDDEN, "CSRF_VALIDATION_FAILED");
    }

    @Test
    void authenticatesAuthMeFromAccessCookie() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE,
                ACCESS_COOKIE + "=" + TestJwtDecoderConfig.VALID_TOKEN);

        ResponseEntity<AuthMeResponse> response = restTemplate.exchange(
                "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                AuthMeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo(TestJwtDecoderConfig.EMAIL);
    }

    @Test
    void bearerHeaderTakesPrecedenceOverAccessCookie() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.INVALID_TOKEN);
        headers.add(HttpHeaders.COOKIE,
                ACCESS_COOKIE + "=" + TestJwtDecoderConfig.VALID_TOKEN);

        ResponseEntity<String> response = restTemplate.exchange(
                "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validBearerHeaderIgnoresInvalidAccessCookie() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.VALID_TOKEN);
        headers.add(HttpHeaders.COOKIE, ACCESS_COOKIE + "=invalid-cookie-token");

        ResponseEntity<AuthMeResponse> response = restTemplate.exchange(
                "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                AuthMeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<CsrfMetadataResponse> csrf() {
        return restTemplate.getForEntity(
                "/auth/session/csrf",
                CsrfMetadataResponse.class);
    }

    private CsrfContext csrfContext() {
        ResponseEntity<CsrfMetadataResponse> response = csrf();
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        String cookie = setCookie.substring(0, setCookie.indexOf(';'));
        return new CsrfContext(response.getBody(), cookie);
    }

    private ResponseEntity<MfaLoginChallengeResponse> login(CsrfContext csrf) {
        return restTemplate.exchange(
                "/auth/session/login",
                HttpMethod.POST,
                new HttpEntity<>(new SessionLoginRequest(EMAIL, PASSWORD), csrf.headers()),
                MfaLoginChallengeResponse.class);
    }

    private ResponseEntity<ErrorResponse> loginError(CsrfContext csrf) {
        return restTemplate.exchange(
                "/auth/session/login",
                HttpMethod.POST,
                new HttpEntity<>(new SessionLoginRequest(EMAIL, PASSWORD), csrf.headers()),
                ErrorResponse.class);
    }

    private <T> ResponseEntity<T> verifyMfa(
            CsrfContext csrf,
            UUID challengeId,
            String code,
            Class<T> responseType) {
        return restTemplate.exchange(
                "/auth/session/mfa/verify",
                HttpMethod.POST,
                new HttpEntity<>(new MfaVerifyRequest(challengeId, code), csrf.headers()),
                responseType);
    }

    private void stubLogin(KeycloakTokenResponse tokenResponse) {
        when(keycloakSessionClient.login(EMAIL, PASSWORD)).thenReturn(tokenResponse);
    }

    private KeycloakTokenResponse tokens(String accessToken, String refreshToken) {
        return new KeycloakTokenResponse(accessToken, refreshToken, 300, 1800);
    }

    private UserTotpCredentialEntity enableCredential(String secret) {
        EncryptedValue encrypted = encryptionService.encrypt(secret);
        return credentialRepository.saveAndFlush(UserTotpCredentialEntity.enabled(
                platformAdminId(),
                encrypted.ciphertext(),
                encrypted.nonce(),
                clock.instant()));
    }

    private UUID platformAdminId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM iam.users WHERE lower(email) = ?",
                UUID.class,
                EMAIL);
    }

    private String currentCode(String secret) {
        try {
            long bucket = Math.floorDiv(clock.instant().getEpochSecond(), 30);
            return codeGenerator.generate(secret, bucket);
        } catch (CodeGenerationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String differentCode(String code) {
        return code.equals("000000") ? "000001" : "000000";
    }

    private void assertTokenCookie(
            ResponseEntity<?> response,
            String name,
            String value,
            long maxAge) {
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
                .anySatisfy(cookie -> assertThat(cookie)
                        .startsWith(name + "=" + value)
                        .contains("Path=/")
                        .contains("Max-Age=" + maxAge)
                        .contains("HttpOnly")
                        .contains("SameSite=Lax")
                        .doesNotContain("Secure"));
    }

    private void assertMfaTokenCookie(
            ResponseEntity<?> response,
            String name,
            String value,
            long minimumMaxAge,
            long maximumMaxAge) {
        String cookie = sessionCookies(response).stream()
                .filter(candidate -> candidate.startsWith(name + "=" + value))
                .findFirst()
                .orElseThrow();
        assertThat(cookie)
                .contains("Path=/")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Secure");
        long maxAge = Long.parseLong(cookie
                .substring(cookie.indexOf("Max-Age=") + 8)
                .split(";", 2)[0]);
        assertThat(maxAge).isBetween(minimumMaxAge, maximumMaxAge);
    }

    private void assertNoSessionCookies(ResponseEntity<?> response) {
        assertThat(sessionCookies(response))
                .noneMatch(cookie -> cookie.startsWith(ACCESS_COOKIE + "=")
                        || cookie.startsWith(REFRESH_COOKIE + "="));
    }

    private List<String> sessionCookies(ResponseEntity<?> response) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        return cookies == null ? List.of() : cookies;
    }

    private void assertClearedCookie(ResponseEntity<?> response, String name) {
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
                .anySatisfy(cookie -> assertThat(cookie)
                        .startsWith(name + "=")
                        .contains("Path=/")
                        .contains("Max-Age=0")
                        .contains("HttpOnly")
                        .contains("SameSite=Lax"));
    }

    private void assertError(
            ResponseEntity<ErrorResponse> response,
            HttpStatus status,
            String errorCode) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(errorCode);
    }

    private record CsrfContext(CsrfMetadataResponse metadata, String cookie) {

        private HttpHeaders headers() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.COOKIE, cookie);
            headers.set(metadata.headerName(), metadata.token());
            return headers;
        }
    }
}
