package com.aliozcan.airportops.flight_service.task;

import java.util.Map;
import java.util.Set;

public enum TaskStatus {
    OPEN,
    IN_PROGRESS,
    DONE,
    BLOCKED;

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            OPEN, Set.of(IN_PROGRESS),
            IN_PROGRESS, Set.of(DONE, BLOCKED),
            BLOCKED, Set.of(IN_PROGRESS),
            DONE, Set.of()
    );

    public boolean canTransitionTo(TaskStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
