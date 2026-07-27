package com.aliozcan.airportops.iam_service.tenant.member;

import com.aliozcan.airportops.iam_service.tenant.TenantContext;
import com.aliozcan.airportops.iam_service.tenant.TenantContextResolver;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Shared authorization gate for the W7 organization-scoped member endpoints:
 * the caller must be an AIRLINE_ADMIN of an active organization, and the
 * organization derived from their own membership must match the path's
 * {orgId} — the path parameter itself is never trusted as an authorization
 * source, only as REST routing.
 */
@Service
public class TenantMemberAccessGuard {

    private static final String AIRLINE_ADMIN_ROLE = "AIRLINE_ADMIN";

    private final TenantContextResolver tenantContextResolver;

    public TenantMemberAccessGuard(TenantContextResolver tenantContextResolver) {
        this.tenantContextResolver = tenantContextResolver;
    }

    public TenantContext requireOrganizationAdmin(UUID callerUserId, UUID pathOrganizationId) {
        TenantContext context = tenantContextResolver.resolveActiveTenantContext(callerUserId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Active tenant membership required"));
        if (!context.roles().contains(AIRLINE_ADMIN_ROLE)) {
            throw new AccessDeniedException("AIRLINE_ADMIN role required");
        }
        if (!context.organizationId().equals(pathOrganizationId)) {
            throw new TenantMismatchException();
        }
        return context;
    }
}
