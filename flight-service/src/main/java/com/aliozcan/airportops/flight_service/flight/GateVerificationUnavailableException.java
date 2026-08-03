package com.aliozcan.airportops.flight_service.flight;

/**
 * Thrown when airport-service cannot be reached (or fails with a server
 * error) while verifying an assigned gate. Flight creation must fail loudly
 * here rather than silently assuming the gate is valid.
 */
public class GateVerificationUnavailableException extends RuntimeException {

    public GateVerificationUnavailableException(Throwable cause) {
        super(cause);
    }
}
