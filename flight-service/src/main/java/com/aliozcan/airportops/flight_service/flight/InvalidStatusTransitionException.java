package com.aliozcan.airportops.flight_service.flight;

public class InvalidStatusTransitionException extends RuntimeException {

    private final FlightStatus currentStatus;
    private final FlightStatus attemptedStatus;

    public InvalidStatusTransitionException(FlightStatus currentStatus, FlightStatus attemptedStatus) {
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }

    public FlightStatus getCurrentStatus() {
        return currentStatus;
    }

    public FlightStatus getAttemptedStatus() {
        return attemptedStatus;
    }
}
