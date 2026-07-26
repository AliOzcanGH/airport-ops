package com.aliozcan.airportops.iam_service.tenant;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;
import com.aliozcan.airportops.iam_service.repository.TenantAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.projection.TenantAuthorizationRow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantContextResolverTests {

    private final TenantAuthorizationRepository repository = mock(TenantAuthorizationRepository.class);
    private final TenantContextResolver resolver = new TenantContextResolver(repository);

    @Test
    void resolveActiveTenantContextIsEmptyWhenUserHasNoMembership() {
        UUID userId = UUID.randomUUID();
        when(repository.findTenantAuthorizationByUserId(userId)).thenReturn(List.of());

        assertThat(resolver.resolveActiveTenantContext(userId)).isEmpty();
    }

    @Test
    void resolveActiveTenantContextIsEmptyWhenOrganizationIsOnboardingIncomplete() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        List<TenantAuthorizationRow> rows =
                List.of(row(orgId, "Onboarding Org", "ONBOARDING_INCOMPLETE", "MEMBER", "READ"));
        when(repository.findTenantAuthorizationByUserId(userId)).thenReturn(rows);

        assertThat(resolver.resolveActiveTenantContext(userId)).isEmpty();
    }

    @Test
    void resolveActiveTenantContextAggregatesRolesAndPermissionsForActiveOrganization() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        List<TenantAuthorizationRow> rows = List.of(
                row(orgId, "Active Org", "ACTIVE", "MEMBER", "READ"),
                row(orgId, "Active Org", "ACTIVE", "MEMBER", "WRITE"),
                row(orgId, "Active Org", "ACTIVE", "ADMIN", null));
        when(repository.findTenantAuthorizationByUserId(userId)).thenReturn(rows);

        Optional<TenantContext> context = resolver.resolveActiveTenantContext(userId);

        assertThat(context).isPresent();
        assertThat(context.get().organizationId()).isEqualTo(orgId);
        assertThat(context.get().organizationName()).isEqualTo("Active Org");
        assertThat(context.get().organizationStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(context.get().roles()).containsExactly("ADMIN", "MEMBER");
        assertThat(context.get().permissions()).containsExactly("READ", "WRITE");
    }

    @Test
    void resolveActiveTenantContextRejectsAmbiguousMultiTenantRows() {
        UUID userId = UUID.randomUUID();
        List<TenantAuthorizationRow> rows = List.of(
                row(UUID.randomUUID(), "Org A", "ACTIVE", "MEMBER", "READ"),
                row(UUID.randomUUID(), "Org B", "ACTIVE", "MEMBER", "READ"));
        when(repository.findTenantAuthorizationByUserId(userId)).thenReturn(rows);

        assertThatThrownBy(() -> resolver.resolveActiveTenantContext(userId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resolveOnboardingAirlineAdminContextIsEmptyWithoutRequiredRole() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        List<TenantAuthorizationRow> rows =
                List.of(row(orgId, "Onboarding Org", "ONBOARDING_INCOMPLETE", "MEMBER", "READ"));
        when(repository.findTenantAuthorizationByUserId(userId)).thenReturn(rows);

        assertThat(resolver.resolveOnboardingAirlineAdminContext(userId)).isEmpty();
    }

    @Test
    void resolveOnboardingAirlineAdminContextResolvesWithRequiredRole() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        List<TenantAuthorizationRow> rows = List.of(
                row(orgId, "Onboarding Org", "ONBOARDING_INCOMPLETE", "AIRLINE_ADMIN", "SETUP_WRITE"));
        when(repository.findTenantAuthorizationByUserId(userId)).thenReturn(rows);

        Optional<TenantContext> context = resolver.resolveOnboardingAirlineAdminContext(userId);

        assertThat(context).isPresent();
        assertThat(context.get().organizationStatus()).isEqualTo(OrganizationStatus.ONBOARDING_INCOMPLETE);
        assertThat(context.get().roles()).containsExactly("AIRLINE_ADMIN");
    }

    @Test
    void resolveSessionTenantContextAcceptsBothOnboardingAndActiveOrganizations() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        List<TenantAuthorizationRow> rows =
                List.of(row(orgId, "Onboarding Org", "ONBOARDING_INCOMPLETE", "MEMBER", "READ"));
        when(repository.findTenantAuthorizationByUserId(userId)).thenReturn(rows);

        assertThat(resolver.resolveSessionTenantContext(userId)).isPresent();
    }

    @Test
    void resolveSessionTenantContextIsEmptyForInactiveOrganization() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        List<TenantAuthorizationRow> rows =
                List.of(row(orgId, "Inactive Org", "INACTIVE", "MEMBER", "READ"));
        when(repository.findTenantAuthorizationByUserId(userId)).thenReturn(rows);

        assertThat(resolver.resolveSessionTenantContext(userId)).isEmpty();
    }

    private TenantAuthorizationRow row(
            UUID organizationId, String organizationName, String organizationStatus,
            String roleCode, String permissionCode) {
        TenantAuthorizationRow row = mock(TenantAuthorizationRow.class);
        when(row.getOrganizationId()).thenReturn(organizationId);
        when(row.getOrganizationName()).thenReturn(organizationName);
        when(row.getOrganizationStatus()).thenReturn(organizationStatus);
        when(row.getRoleCode()).thenReturn(roleCode);
        when(row.getPermissionCode()).thenReturn(permissionCode);
        return row;
    }
}
