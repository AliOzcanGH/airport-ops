package com.aliozcan.airportops.airport_service.error;

import com.aliozcan.airportops.airport_service.gate.GateCodeConflictException;
import com.aliozcan.airportops.airport_service.gate.GateNotFoundException;
import com.aliozcan.airportops.airport_service.gate.StationNotFoundException;
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

    @ExceptionHandler(StationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStationNotFound(
            StationNotFoundException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "STATION_NOT_FOUND",
                "Station was not found for the authenticated tenant",
                request);
    }

    @ExceptionHandler(GateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGateNotFound(
            GateNotFoundException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "GATE_NOT_FOUND",
                "Gate was not found for the requested station",
                request);
    }

    @ExceptionHandler(GateCodeConflictException.class)
    public ResponseEntity<ErrorResponse> handleGateCodeConflict(
            GateCodeConflictException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "GATE_CODE_CONFLICT",
                "A gate with this code already exists for the station",
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
