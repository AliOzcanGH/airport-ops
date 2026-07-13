package com.aliozcan.airportops.iam_service.auth.dto;

import com.aliozcan.airportops.iam_service.domain.model.enums.UserStatus;

import java.util.SortedSet;
import java.util.UUID;

public record AuthMeResponse(
        String keycloakSubject,
        String issuer,
        String email,
        String fullName,
        String preferredUsername,
        UUID iamUserId,
        UserStatus iamUserStatus,
        SortedSet<String> keycloakRoles,
        SortedSet<String> iamRoles,
        SortedSet<String> permissions,
        SortedSet<WorkspaceType> availableWorkspaces,
        WorkspaceType defaultWorkspace,
        TenantContextResponse tenantContext
) {
}
