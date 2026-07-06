package com.aliozcan.airportops.iam_service.repository.projection;

import java.util.UUID;

public interface TenantAuthorizationRow {

    UUID getOrganizationId();

    String getOrganizationName();

    String getOrganizationStatus();

    String getRoleCode();

    String getPermissionCode();
}
