package com.aliozcan.airportops.iam_service.security;

import com.aliozcan.airportops.iam_service.config.SessionCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
public class HeaderOrCookieBearerTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver headerResolver =
            new DefaultBearerTokenResolver();
    private final SessionCookieProperties cookieProperties;

    public HeaderOrCookieBearerTokenResolver(SessionCookieProperties cookieProperties) {
        this.cookieProperties = cookieProperties;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
            return headerResolver.resolve(request);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieProperties.accessName().equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
