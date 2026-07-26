package com.aliozcan.airportops.iam_service.tenant;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;
import com.aliozcan.airportops.iam_service.repository.TenantAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.projection.TenantAuthorizationRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Shared tenant-membership lookup. Each public method expresses a distinct
 * caller intent (allowed organization statuses / required role) on top of the
 * same underlying active-membership query, so that intent-specific rules
 * (e.g. onboarding vs. active-dashboard access) stay in one place instead of
 * being re-derived per endpoint.
 */
@Service
public class TenantContextResolver {

    private final TenantAuthorizationRepository tenantAuthorizationRepository;

    public TenantContextResolver(TenantAuthorizationRepository tenantAuthorizationRepository) {
        this.tenantAuthorizationRepository = tenantAuthorizationRepository;
    }

    /**
     * Used by the W6 tenant dashboard: the caller must be an active member of
     * a fully-active organization.
     */
    @Transactional(readOnly = true)
    public Optional<TenantContext> resolveActiveTenantContext(UUID userId) {
        return resolve(userId, EnumSet.of(OrganizationStatus.ACTIVE), null);
    }

    /**
     * Not wired to any endpoint yet — reserved for a future refactor of the
     * app/setup profile & completion flows, which currently require an
     * AIRLINE_ADMIN member of an onboarding-incomplete organization.
     */
    @Transactional(readOnly = true)
    public Optional<TenantContext> resolveOnboardingAirlineAdminContext(UUID userId) {
        return resolve(userId, EnumSet.of(OrganizationStatus.ONBOARDING_INCOMPLETE), "AIRLINE_ADMIN");
    }

    /**
     * Not wired to any endpoint yet — reserved for a future refactor of
     * auth/me, which accepts both onboarding-incomplete and active
     * organizations for session tenant-context derivation.
     */
    @Transactional(readOnly = true)
    public Optional<TenantContext> resolveSessionTenantContext(UUID userId) {
        return resolve(userId,
                EnumSet.of(OrganizationStatus.ONBOARDING_INCOMPLETE, OrganizationStatus.ACTIVE),
                null);
    }

    private Optional<TenantContext> resolve(
            UUID userId, Set<OrganizationStatus> allowedStatuses, String requiredRoleCode) {
        List<TenantAuthorizationRow> rows =
                tenantAuthorizationRepository.findTenantAuthorizationByUserId(userId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        TenantAuthorizationRow first = rows.get(0);
        SortedSet<String> roles = new TreeSet<>();
        SortedSet<String> permissions = new TreeSet<>();
        for (TenantAuthorizationRow row : rows) {
            if (!first.getOrganizationId().equals(row.getOrganizationId())) {
                throw new IllegalStateException(
                        "Active IAM user has multiple tenant organizations");
            }
            if (row.getRoleCode() != null) {
                roles.add(row.getRoleCode());
            }
            if (row.getPermissionCode() != null) {
                permissions.add(row.getPermissionCode());
            }
        }

        OrganizationStatus status = OrganizationStatus.valueOf(first.getOrganizationStatus());
        if (!allowedStatuses.contains(status)) {
            return Optional.empty();
        }
        if (requiredRoleCode != null && !roles.contains(requiredRoleCode)) {
            return Optional.empty();
        }

        return Optional.of(new TenantContext(
                first.getOrganizationId(), first.getOrganizationName(), status, roles, permissions));
    }
}
