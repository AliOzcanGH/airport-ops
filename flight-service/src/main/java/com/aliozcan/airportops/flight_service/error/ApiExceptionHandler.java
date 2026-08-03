package com.aliozcan.airportops.flight_service.error;

import com.aliozcan.airportops.flight_service.flight.FlightNumberConflictException;
import com.aliozcan.airportops.flight_service.flight.GateConflictException;
import com.aliozcan.airportops.flight_service.flight.GateNotActiveException;
import com.aliozcan.airportops.flight_service.flight.GateNotFoundException;
import com.aliozcan.airportops.flight_service.flight.GateVerificationUnavailableException;
import com.aliozcan.airportops.flight_service.flight.TenantMismatchException;
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

    @ExceptionHandler(GateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGateNotFound(
            GateNotFoundException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "GATE_NOT_FOUND",
                "Assigned gate was not found for this organization",
                request);
    }

    @ExceptionHandler(GateNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleGateNotActive(
            GateNotActiveException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "GATE_NOT_ACTIVE",
                "Assigned gate is not currently ACTIVE",
                request);
    }

    @ExceptionHandler(GateConflictException.class)
    public ResponseEntity<ErrorResponse> handleGateConflict(
            GateConflictException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "GATE_CONFLICT",
                "Another flight already occupies this gate during the requested time range",
                request);
    }

    @ExceptionHandler(FlightNumberConflictException.class)
    public ResponseEntity<ErrorResponse> handleFlightNumberConflict(
            FlightNumberConflictException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "FLIGHT_NUMBER_CONFLICT",
                "A flight with this number and departure time already exists",
                request);
    }

    @ExceptionHandler(GateVerificationUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleGateVerificationUnavailable(
            GateVerificationUnavailableException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.BAD_GATEWAY,
                "GATE_VERIFICATION_UNAVAILABLE",
                "Could not verify the assigned gate because airport-service is unreachable",
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
