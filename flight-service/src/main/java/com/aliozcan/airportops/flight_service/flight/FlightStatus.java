package com.aliozcan.airportops.flight_service.flight;

import java.util.Map;
import java.util.Set;

public enum FlightStatus {
    SCHEDULED,
    BOARDING,
    DEPARTED,
    DELAYED,
    CANCELLED;

    private static final Map<FlightStatus, Set<FlightStatus>> ALLOWED_TRANSITIONS = Map.of(
            SCHEDULED, Set.of(BOARDING, DELAYED, CANCELLED),
            BOARDING, Set.of(DEPARTED),
            DELAYED, Set.of(BOARDING, CANCELLED),
            DEPARTED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canTransitionTo(FlightStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
