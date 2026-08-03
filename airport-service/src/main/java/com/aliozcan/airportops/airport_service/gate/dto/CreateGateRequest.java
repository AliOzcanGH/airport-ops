package com.aliozcan.airportops.airport_service.gate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGateRequest(
        @NotBlank @Size(max = 10) String code,
        @Size(max = 50) String terminal
) {
}
