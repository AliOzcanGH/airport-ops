package com.aliozcan.airportops.iam_service.auth.session;

import com.aliozcan.airportops.iam_service.auth.InvalidLoginException;
import com.aliozcan.airportops.iam_service.auth.dto.AuthMeResponse;
import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.auth.session.dto.CsrfMetadataResponse;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionAuthIntegrationTests {

    private static final String ACCESS_COOKIE = "airport_ops_access_token";
    private static final String REFRESH_COOKIE = "airport_ops_refresh_token";

    @Autowired
    private TestRestTemplate restTemplate;

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
    void loginSetsHttpOnlyCookiesWithoutReturningTokens() {
        when(keycloakSessionClient.login("platform.admin@demo.com", "Admin123!"))
                .thenReturn(tokens("access-token", "refresh-token"));
        CsrfContext csrf = csrfContext();

        ResponseEntity<String> response = restTemplate.exchange(
                "/auth/session/login",
                HttpMethod.POST,
                new HttpEntity<>(
                        new SessionLoginRequest(
                                "  platform.admin@demo.com  ",
                                "Admin123!"),
                        csrf.headers()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        assertTokenCookie(response, ACCESS_COOKIE, "access-token", 300);
        assertTokenCookie(response, REFRESH_COOKIE, "refresh-token", 1800);
        verify(keycloakSessionClient)
                .login("platform.admin@demo.com", "Admin123!");
    }

    @Test
    void invalidLoginReturnsGenericUnauthorizedResponse() {
        when(keycloakSessionClient.login(anyString(), anyString()))
                .thenThrow(new InvalidLoginException());

        ResponseEntity<ErrorResponse> response = login(csrfContext(), ErrorResponse.class);

        assertError(response, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isNullOrEmpty();
    }

    @Test
    void unavailableProviderReturnsServiceUnavailable() {
        when(keycloakSessionClient.login(anyString(), anyString()))
                .thenThrow(new AuthProviderUnavailableException(
                        new IllegalStateException("unavailable")));

        ResponseEntity<ErrorResponse> response = login(csrfContext(), ErrorResponse.class);

        assertError(response, HttpStatus.SERVICE_UNAVAILABLE, "AUTH_PROVIDER_UNAVAILABLE");
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
                new SessionLoginRequest("platform.admin@demo.com", "Admin123!"),
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

    private <T> ResponseEntity<T> login(CsrfContext csrf, Class<T> responseType) {
        return restTemplate.exchange(
                "/auth/session/login",
                HttpMethod.POST,
                new HttpEntity<>(
                        new SessionLoginRequest(
                                "platform.admin@demo.com",
                                "Admin123!"),
                        csrf.headers()),
                responseType);
    }

    private KeycloakTokenResponse tokens(String accessToken, String refreshToken) {
        return new KeycloakTokenResponse(accessToken, refreshToken, 300, 1800);
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
