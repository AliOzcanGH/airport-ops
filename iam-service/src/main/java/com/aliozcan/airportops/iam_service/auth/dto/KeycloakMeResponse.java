package com.aliozcan.airportops.iam_service.auth.dto;

import java.util.SortedSet;

public record KeycloakMeResponse(
        String subject,
        String email,
        String preferredUsername,
        String issuer,
        SortedSet<String> roles
) {
}
