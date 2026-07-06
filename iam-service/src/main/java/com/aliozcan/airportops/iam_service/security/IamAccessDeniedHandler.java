package com.aliozcan.airportops.iam_service.security;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class IamAccessDeniedHandler implements AccessDeniedHandler {

    private static final String USER_NOT_PROVISIONED_CODE = "USER_NOT_PROVISIONED";
    private static final String USER_NOT_PROVISIONED_MESSAGE =
            "Authenticated user is not provisioned in IAM";
    private static final String MISSING_PERMISSION_CODE = "MISSING_PERMISSION";
    private static final String MISSING_PERMISSION_MESSAGE =
            "Authenticated user does not have the required permission";
    private static final String CSRF_VALIDATION_FAILED_CODE = "CSRF_VALIDATION_FAILED";
    private static final String CSRF_VALIDATION_FAILED_MESSAGE =
            "CSRF token is missing or invalid";

    private final ObjectMapper objectMapper;

    public IamAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        HttpStatus status = HttpStatus.FORBIDDEN;
        boolean csrfFailure = accessDeniedException instanceof CsrfException;
        boolean unprovisioned = !csrfFailure && isUnprovisioned(authentication());
        String errorCode = csrfFailure
                ? CSRF_VALIDATION_FAILED_CODE
                : unprovisioned ? USER_NOT_PROVISIONED_CODE : MISSING_PERMISSION_CODE;
        String message = csrfFailure
                ? CSRF_VALIDATION_FAILED_MESSAGE
                : unprovisioned ? USER_NOT_PROVISIONED_MESSAGE : MISSING_PERMISSION_MESSAGE;

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                errorCode,
                message,
                request.getRequestURI()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean isUnprovisioned(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        Object details = authentication.getDetails();
        return details instanceof IamAuthenticationDetails iamDetails
                && !iamDetails.provisioned();
    }
}
