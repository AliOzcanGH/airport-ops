package com.aliozcan.airportops.airport_service.station.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateStationRequest(
        @NotBlank String stationName,
        @NotBlank String airportCode,
        @Min(0) int gateCount
) {
}
