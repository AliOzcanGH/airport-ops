package com.aliozcan.airportops.iam_service.platform.tenant.dto;

import java.util.List;

public record PlatformTenantDirectoryResponse(
        List<PlatformTenantSummaryResponse> tenants
) {
}
