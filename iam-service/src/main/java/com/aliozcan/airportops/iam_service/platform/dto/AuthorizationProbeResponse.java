package com.aliozcan.airportops.iam_service.platform.dto;

public record AuthorizationProbeResponse(
        String message,
        String requiredPermission
) {
}
