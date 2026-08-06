package com.aliozcan.airportops.flight_service.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTaskStatusRequest(
        @NotBlank @Pattern(regexp = "OPEN|IN_PROGRESS|DONE|BLOCKED") String status,
        @Size(max = 150) String assignedTo
) {
}
