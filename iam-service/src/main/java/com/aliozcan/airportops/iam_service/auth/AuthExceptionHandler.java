package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class AuthExceptionHandler {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";
    private static final String USER_NOT_PROVISIONED_MESSAGE =
            "Authenticated user is not provisioned in IAM";

    @ExceptionHandler(InvalidLoginException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLogin(
            InvalidLoginException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                "INVALID_CREDENTIALS",
                INVALID_CREDENTIALS_MESSAGE,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(UserNotProvisionedException.class)
    public ResponseEntity<ErrorResponse> handleUserNotProvisioned(
            UserNotProvisionedException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                "USER_NOT_PROVISIONED",
                USER_NOT_PROVISIONED_MESSAGE,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }
}
