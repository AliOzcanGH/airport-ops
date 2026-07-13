package com.aliozcan.airportops.iam_service.repository.projection;

import java.time.Instant;
import java.util.UUID;

public interface PlatformTenantSummaryRow {

    UUID getOrganizationId();

    String getOrganizationName();

    String getOrganizationStatus();

    Instant getCreatedAt();

    Long getMemberCount();

    String getPrimaryAdminEmail();
}
