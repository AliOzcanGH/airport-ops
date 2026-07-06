package com.aliozcan.airportops.iam_service.security;

import com.aliozcan.airportops.iam_service.config.SessionCookieProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeaderOrCookieBearerTokenResolverTests {

    private final HeaderOrCookieBearerTokenResolver resolver =
            new HeaderOrCookieBearerTokenResolver(properties());

    @Test
    void usesCookieOnlyWhenAuthorizationHeaderIsAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("airport_ops_access_token", "cookie-token"));

        assertThat(resolver.resolve(request)).isEqualTo("cookie-token");
    }

    @Test
    void bearerHeaderTakesPrecedenceOverCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token");
        request.setCookies(new Cookie("airport_ops_access_token", "cookie-token"));

        assertThat(resolver.resolve(request)).isEqualTo("header-token");
    }

    @Test
    void malformedBearerHeaderDoesNotFallBackToCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer");
        request.setCookies(new Cookie("airport_ops_access_token", "cookie-token"));

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void nonBearerAuthorizationHeaderDoesNotFallBackToCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic credentials");
        request.setCookies(new Cookie("airport_ops_access_token", "cookie-token"));

        assertThat(resolver.resolve(request)).isNull();
    }

    private SessionCookieProperties properties() {
        return new SessionCookieProperties(
                "airport_ops_access_token",
                "airport_ops_refresh_token",
                "Lax",
                false);
    }
}
