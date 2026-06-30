package com.aliozcan.airportops.iam_service.auth.dto;

import java.util.SortedSet;
import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String email,
        String fullName,
        String tokenScope,
        SortedSet<String> roles,
        SortedSet<String> permissions
) {
}
