package com.aliozcan.airportops.airport_service.gate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateGateStatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|MAINTENANCE|CLOSED") String status
) {
}
