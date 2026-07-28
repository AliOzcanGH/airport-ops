package com.aliozcan.airportops.iam_service.auth.dto;

public record IamTokenResponse(
        String iamAccessToken,
        long expiresIn
) {
}
