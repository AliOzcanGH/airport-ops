package com.aliozcan.airportops.flight_service.flight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateFlightRequest(
        @NotBlank @Size(max = 10) String flightNumber,
        @NotBlank @Size(max = 10) String origin,
        @NotBlank @Size(max = 10) String destination,
        @NotNull Instant scheduledDeparture,
        @NotNull Instant scheduledArrival,
        @NotNull UUID assignedGateId
) {
}
