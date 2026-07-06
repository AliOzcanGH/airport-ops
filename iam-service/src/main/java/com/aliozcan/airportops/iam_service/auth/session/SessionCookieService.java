package com.aliozcan.airportops.iam_service.auth.session;

import com.aliozcan.airportops.iam_service.config.SessionCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SessionCookieService {

    private final SessionCookieProperties properties;

    public SessionCookieService(SessionCookieProperties properties) {
        this.properties = properties;
    }

    public void writeTokens(
            HttpServletResponse response,
            KeycloakTokenResponse tokens) {
        addCookie(response, properties.accessName(), tokens.accessToken(), tokens.expiresIn());
        addCookie(
                response,
                properties.refreshName(),
                tokens.refreshToken(),
                tokens.refreshExpiresIn());
    }

    public void clearTokens(HttpServletResponse response) {
        addCookie(response, properties.accessName(), "", 0);
        addCookie(response, properties.refreshName(), "", 0);
    }

    public String readRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (properties.refreshName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.secure())
                .path("/")
                .sameSite(properties.sameSite())
                .maxAge(Duration.ofSeconds(Math.max(0, maxAgeSeconds)))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
