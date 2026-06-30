package com.aliozcan.airportops.iam_service.auth.dto;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String errorCode,
        String message,
        String path
) {
}
