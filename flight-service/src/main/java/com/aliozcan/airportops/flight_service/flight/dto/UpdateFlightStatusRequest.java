package com.aliozcan.airportops.flight_service.flight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateFlightStatusRequest(
        @NotBlank @Pattern(regexp = "SCHEDULED|BOARDING|DEPARTED|DELAYED|CANCELLED") String status
) {
}
