package com.aliozcan.airportops.iam_service.security;

import com.aliozcan.airportops.iam_service.config.SessionCookieProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SessionCsrfRequestMatcherTests {

    private final SessionCsrfRequestMatcher matcher =
            new SessionCsrfRequestMatcher(properties());

    @Test
    void protectsSessionUnsafeEndpointsWithoutCookies() {
        MockHttpServletRequest request = post("/auth/session/login");

        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void protectsUnsafeRequestsWithAmbientCookie() {
        MockHttpServletRequest request = post("/platform/invitations");
        request.setCookies(new Cookie("airport_ops_access_token", "token"));

        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void explicitBearerRequestRemainsCsrfIndependent() {
        MockHttpServletRequest request = post("/platform/invitations");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        request.setCookies(new Cookie("airport_ops_access_token", "cookie-token"));

        assertThat(matcher.matches(request)).isFalse();
    }

    @Test
    void legacyLoginRemainsCsrfIndependent() {
        MockHttpServletRequest request = post("/auth/login");
        request.setCookies(new Cookie("airport_ops_access_token", "cookie-token"));

        assertThat(matcher.matches(request)).isFalse();
    }

    @Test
    void publicInvitationWithoutCookiesRemainsCsrfIndependent() {
        assertThat(matcher.matches(post("/invitations/accept"))).isFalse();
    }

    private MockHttpServletRequest post(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        return request;
    }

    private SessionCookieProperties properties() {
        return new SessionCookieProperties(
                "airport_ops_access_token",
                "airport_ops_refresh_token",
                "Lax",
                false);
    }
}
