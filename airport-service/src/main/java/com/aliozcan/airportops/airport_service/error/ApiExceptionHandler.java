package com.aliozcan.airportops.airport_service.error;

import com.aliozcan.airportops.airport_service.station.TenantMismatchException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(TenantMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTenantMismatch(
            TenantMismatchException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.FORBIDDEN,
                "TENANT_MISMATCH",
                "Requested organization does not match the authenticated tenant",
                request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.FORBIDDEN,
                "MISSING_PERMISSION",
                "Authenticated principal lacks the required permission",
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                request);
    }

    private ResponseEntity<ErrorResponse> errorResponse(
            HttpStatus status, String errorCode, String message, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                errorCode,
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(response);
    }
}
