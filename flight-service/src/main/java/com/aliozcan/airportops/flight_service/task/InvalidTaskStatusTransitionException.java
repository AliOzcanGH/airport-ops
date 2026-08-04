package com.aliozcan.airportops.flight_service.task;

public class InvalidTaskStatusTransitionException extends RuntimeException {

    private final TaskStatus currentStatus;
    private final TaskStatus attemptedStatus;

    public InvalidTaskStatusTransitionException(TaskStatus currentStatus, TaskStatus attemptedStatus) {
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }

    public TaskStatus getCurrentStatus() {
        return currentStatus;
    }

    public TaskStatus getAttemptedStatus() {
        return attemptedStatus;
    }
}
