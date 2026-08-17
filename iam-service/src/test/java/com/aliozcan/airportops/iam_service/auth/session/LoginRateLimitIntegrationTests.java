package com.aliozcan.airportops.iam_service.auth.session;

import com.aliozcan.airportops.iam_service.auth.InvalidLoginException;
import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.auth.session.dto.CsrfMetadataResponse;
import com.aliozcan.airportops.iam_service.auth.session.dto.MfaLoginChallengeResponse;
import com.aliozcan.airportops.iam_service.auth.session.dto.SessionLoginRequest;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * W17 — proves the login-attempt lockout (LoginAttemptGuard) actually
 * blocks repeated /auth/session/login failures with 429, and that it
 * stays locked even for a subsequently correct password.
 *
 * Each test uses a unique email so the shared in-memory LoginAttemptGuard
 * (a singleton bean reused across the whole test JVM) can't leak state
 * between tests.
 */
@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginRateLimitIntegrationTests {

    private static final String PASSWORD = "Wr0ngPassword!";
    private static final String CORRECT_PASSWORD = "Correct123!";
    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private KeycloakSessionClient keycloakSessionClient;

    @BeforeEach
    void resetClient() {
        reset(keycloakSessionClient);
    }

    @Test
    void locksLoginAfterRepeatedFailuresThenReturns429() {
        String email = uniqueEmail("lockout");
        when(keycloakSessionClient.login(eq(email), anyString()))
                .thenThrow(new InvalidLoginException());

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            ResponseEntity<ErrorResponse> response = login(email, PASSWORD);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().errorCode()).isEqualTo("INVALID_CREDENTIALS");
        }

        ResponseEntity<ErrorResponse> lockedResponse = login(email, PASSWORD);
        assertThat(lockedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(lockedResponse.getBody().errorCode()).isEqualTo("LOGIN_LOCKED");
    }

    @Test
    void lockoutRejectsSubsequentCorrectPasswordUntilItExpires() {
        String email = uniqueEmail("lockout-correct");
        insertActiveUser(email);
        when(keycloakSessionClient.login(eq(email), eq(PASSWORD)))
                .thenThrow(new InvalidLoginException());
        when(keycloakSessionClient.login(eq(email), eq(CORRECT_PASSWORD)))
                .thenReturn(new KeycloakTokenResponse("access", "refresh", 900, 1800));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            login(email, PASSWORD);
        }

        ResponseEntity<ErrorResponse> response = login(email, CORRECT_PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().errorCode()).isEqualTo("LOGIN_LOCKED");
    }

    @Test
    void successfulLoginDoesNotCountTowardLockout() {
        String email = uniqueEmail("healthy");
        insertActiveUser(email);
        when(keycloakSessionClient.login(eq(email), eq(CORRECT_PASSWORD)))
                .thenReturn(new KeycloakTokenResponse("access", "refresh", 900, 1800));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS + 2; attempt++) {
            ResponseEntity<MfaLoginChallengeResponse> response = login(email, CORRECT_PASSWORD, MfaLoginChallengeResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    private ResponseEntity<ErrorResponse> login(String email, String password) {
        return login(email, password, ErrorResponse.class);
    }

    private <T> ResponseEntity<T> login(String email, String password, Class<T> responseType) {
        CsrfContext csrf = csrfContext();
        return restTemplate.exchange(
                "/auth/session/login",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(new SessionLoginRequest(email, password), csrf.headers()),
                responseType);
    }

    private ResponseEntity<CsrfMetadataResponse> csrf() {
        return restTemplate.getForEntity("/auth/session/csrf", CsrfMetadataResponse.class);
    }

    private CsrfContext csrfContext() {
        ResponseEntity<CsrfMetadataResponse> response = csrf();
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        String cookie = setCookie.substring(0, setCookie.indexOf(';'));
        return new CsrfContext(response.getBody(), cookie);
    }

    private String uniqueEmail(String label) {
        return "w17." + label + "." + UUID.randomUUID() + "@ratelimit.test";
    }

    private void insertActiveUser(String email) {
        jdbcTemplate.update(
                """
                        INSERT INTO iam.users (
                            email, password_hash, full_name, status, auth_provider
                        ) VALUES (?, NULL, 'W17 Rate Limit Test User', 'ACTIVE', 'KEYCLOAK')
                        """,
                email);
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
