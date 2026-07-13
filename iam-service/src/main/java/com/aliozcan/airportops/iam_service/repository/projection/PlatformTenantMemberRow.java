package com.aliozcan.airportops.iam_service.repository.projection;

import java.time.Instant;
import java.util.UUID;

public interface PlatformTenantMemberRow {

    UUID getMemberId();

    UUID getUserId();

    String getEmail();

    String getFullName();

    String getMemberStatus();

    Instant getJoinedAt();

    String getRoleCode();
}
