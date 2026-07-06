package com.aliozcan.airportops.iam_service.auth.session.dto;

public record CsrfMetadataResponse(
        String headerName,
        String parameterName,
        String token
) {
}
