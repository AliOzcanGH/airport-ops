package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.AuthMeResponse;
import com.aliozcan.airportops.iam_service.auth.dto.TenantContextResponse;
import com.aliozcan.airportops.iam_service.auth.dto.WorkspaceType;
import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;
import com.aliozcan.airportops.iam_service.repository.PlatformAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.TenantAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformAuthorizationRow;
import com.aliozcan.airportops.iam_service.repository.projection.TenantAuthorizationRow;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@Service
public class AuthMeService {

    private final UserRepository userRepository;
    private final PlatformAuthorizationRepository platformAuthorizationRepository;
    private final TenantAuthorizationRepository tenantAuthorizationRepository;
    private final KeycloakRealmRoleExtractor roleExtractor;

    public AuthMeService(
            UserRepository userRepository,
            PlatformAuthorizationRepository platformAuthorizationRepository,
            TenantAuthorizationRepository tenantAuthorizationRepository,
            KeycloakRealmRoleExtractor roleExtractor) {
        this.userRepository = userRepository;
        this.platformAuthorizationRepository = platformAuthorizationRepository;
        this.tenantAuthorizationRepository = tenantAuthorizationRepository;
        this.roleExtractor = roleExtractor;
    }

    @Transactional(readOnly = true)
    public AuthMeResponse getCurrentUser(Jwt jwt) {
        String email = normalizedEmail(jwt);
        UserEntity user = userRepository.findActiveByEmail(email)
                .orElseThrow(UserNotProvisionedException::new);

        List<PlatformAuthorizationRow> authorizationRows =
                platformAuthorizationRepository.findPlatformAuthorizationByUserId(user.getId());

        SortedSet<String> iamRoles = new TreeSet<>();
        SortedSet<String> permissions = new TreeSet<>();
        for (PlatformAuthorizationRow row : authorizationRows) {
            if (row.getRoleCode() != null) {
                iamRoles.add(row.getRoleCode());
            }
            if (row.getPermissionCode() != null) {
                permissions.add(row.getPermissionCode());
            }
        }

        List<TenantAuthorizationRow> tenantRows =
                tenantAuthorizationRepository.findTenantAuthorizationByUserId(user.getId());
        TenantContextResponse tenantContext = tenantContext(tenantRows);
        SortedSet<WorkspaceType> availableWorkspaces = new TreeSet<>();
        if (!iamRoles.isEmpty()) {
            availableWorkspaces.add(WorkspaceType.PLATFORM);
        }
        if (tenantContext != null) {
            availableWorkspaces.add(WorkspaceType.TENANT);
        }
        WorkspaceType defaultWorkspace = availableWorkspaces.contains(WorkspaceType.PLATFORM)
                ? WorkspaceType.PLATFORM
                : availableWorkspaces.contains(WorkspaceType.TENANT)
                ? WorkspaceType.TENANT
                : null;

        return new AuthMeResponse(
                jwt.getSubject(),
                jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
                email,
                user.getFullName(),
                user.getPreferredLanguage(),
                jwt.getClaimAsString("preferred_username"),
                user.getId(),
                user.getStatus(),
                roleExtractor.extract(jwt),
                iamRoles,
                permissions,
                availableWorkspaces,
                defaultWorkspace,
                tenantContext
        );
    }

    private TenantContextResponse tenantContext(List<TenantAuthorizationRow> rows) {
        if (rows.isEmpty()) {
            return null;
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
        return new TenantContextResponse(
                first.getOrganizationId(),
                first.getOrganizationName(),
                OrganizationStatus.valueOf(first.getOrganizationStatus()),
                roles,
                permissions);
    }

    private String normalizedEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotProvisionedException();
        }
        return email.trim();
    }
}
