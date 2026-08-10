package com.aliozcan.airportops.audit_service.security;

import java.util.SortedSet;
import java.util.UUID;

public record IamPrincipal(
        UUID organizationId,
        String workspace,
        SortedSet<String> roles,
        SortedSet<String> permissions) {
}
