package com.aliozcan.airportops.report_service.security;

import java.util.SortedSet;
import java.util.UUID;

public record IamPrincipal(
        UUID organizationId,
        String workspace,
        SortedSet<String> roles,
        SortedSet<String> permissions) {
}
