package com.aliozcan.airportops.iam_service.platform.tenant;

import com.aliozcan.airportops.iam_service.platform.tenant.dto.PlatformTenantDetailResponse;
import com.aliozcan.airportops.iam_service.platform.tenant.dto.PlatformTenantDirectoryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/platform/tenants")
public class PlatformTenantController {

    private final PlatformTenantDirectoryService service;

    public PlatformTenantController(PlatformTenantDirectoryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('tenant:read')")
    public PlatformTenantDirectoryResponse listTenants() {
        return service.listTenants();
    }

    @GetMapping("/{organizationId}")
    @PreAuthorize("hasAuthority('tenant:read')")
    public PlatformTenantDetailResponse getTenant(
            @PathVariable UUID organizationId) {
        return service.getTenantDetail(organizationId);
    }
}
