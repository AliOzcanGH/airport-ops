package com.aliozcan.airportops.audit_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Guards /internal/** endpoints with a static shared secret instead of an
 * IAM-issued JWT — these are called service-to-service (e.g. iam-service
 * writing an audit entry directly after a role-update commit), not by an
 * end-user session.
 */
@Component
@EnableConfigurationProperties(InternalServiceSecretProperties.class)
public class InternalServiceSecretFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Service-Secret";

    private final InternalServiceSecretProperties properties;

    public InternalServiceSecretFilter(InternalServiceSecretProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String providedSecret = request.getHeader(HEADER_NAME);
        if (providedSecret == null || !providedSecret.equals(properties.internalServiceSecret())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"errorCode\":\"INVALID_INTERNAL_SECRET\",\"message\":\"Missing or invalid "
                            + HEADER_NAME + " header\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
