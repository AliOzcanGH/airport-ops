package com.aliozcan.airportops.iam_service.security;

import com.aliozcan.airportops.iam_service.config.SessionCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SessionCsrfRequestMatcher implements RequestMatcher {

    private static final Set<String> SAFE_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.TRACE.name(),
            HttpMethod.OPTIONS.name());

    private final SessionCookieProperties cookieProperties;

    public SessionCsrfRequestMatcher(SessionCookieProperties cookieProperties) {
        this.cookieProperties = cookieProperties;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return false;
        }
        if (request.getRequestURI().startsWith("/auth/session/")) {
            return true;
        }
        if ("/auth/login".equals(request.getRequestURI())) {
            return false;
        }
        if (hasBearerAuthorization(request)) {
            return false;
        }
        return hasSessionCookie(request);
    }

    private boolean hasBearerAuthorization(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null
                && authorization.trim().regionMatches(true, 0, "Bearer", 0, 6);
    }

    private boolean hasSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (cookieProperties.accessName().equals(cookie.getName())
                    || cookieProperties.refreshName().equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }
}
