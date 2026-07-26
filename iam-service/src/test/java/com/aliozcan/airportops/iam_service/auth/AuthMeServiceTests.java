package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.AuthMeResponse;
import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.PreferredLanguage;
import com.aliozcan.airportops.iam_service.domain.model.enums.UserStatus;
import com.aliozcan.airportops.iam_service.repository.PlatformAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.TenantAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.repository.projection.TenantAuthorizationRow;
import com.aliozcan.airportops.iam_service.tenant.AmbiguousTenantContextException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthMeServiceTests {

    @Test
    void returnsEmptyIamAuthorizationWhenProvisionedUserHasNoPlatformRoles() {
        UserRepository userRepository = mock(UserRepository.class);
        PlatformAuthorizationRepository authorizationRepository =
                mock(PlatformAuthorizationRepository.class);
        TenantAuthorizationRepository tenantAuthorizationRepository =
                mock(TenantAuthorizationRepository.class);
        KeycloakRealmRoleExtractor roleExtractor = new KeycloakRealmRoleExtractor();
        UserEntity user = mock(UserEntity.class);
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwt(Map.of(
                "email", "platform.admin@demo.com",
                "realm_access", Map.of("roles", List.of("PLATFORM_ADMIN"))
        ));

        when(user.getId()).thenReturn(userId);
        when(user.getFullName()).thenReturn("Platform Admin");
        when(user.getPreferredLanguage()).thenReturn(PreferredLanguage.EN);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findActiveByEmail("platform.admin@demo.com"))
                .thenReturn(Optional.of(user));
        when(authorizationRepository.findPlatformAuthorizationByUserId(userId))
                .thenReturn(List.of());
        when(tenantAuthorizationRepository.findTenantAuthorizationByUserId(userId))
                .thenReturn(List.of());

        AuthMeService service = new AuthMeService(
                userRepository,
                authorizationRepository,
                tenantAuthorizationRepository,
                roleExtractor);

        AuthMeResponse response = service.getCurrentUser(jwt);

        assertThat(response.iamRoles()).isEmpty();
        assertThat(response.fullName()).isEqualTo("Platform Admin");
        assertThat(response.preferredLanguage()).isEqualTo(PreferredLanguage.EN);
        assertThat(response.permissions()).isEmpty();
        assertThat(response.keycloakRoles()).containsExactly("PLATFORM_ADMIN");
        assertThat(response.availableWorkspaces()).isEmpty();
        assertThat(response.defaultWorkspace()).isNull();
        assertThat(response.tenantContext()).isNull();
    }

    @Test
    void throwsAmbiguousTenantContextWhenUserHasRowsAcrossMultipleOrganizations() {
        UserRepository userRepository = mock(UserRepository.class);
        PlatformAuthorizationRepository authorizationRepository =
                mock(PlatformAuthorizationRepository.class);
        TenantAuthorizationRepository tenantAuthorizationRepository =
                mock(TenantAuthorizationRepository.class);
        KeycloakRealmRoleExtractor roleExtractor = new KeycloakRealmRoleExtractor();
        UserEntity user = mock(UserEntity.class);
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwt(Map.of("email", "platform.admin@demo.com"));

        TenantAuthorizationRow rowA = mock(TenantAuthorizationRow.class);
        when(rowA.getOrganizationId()).thenReturn(UUID.randomUUID());
        TenantAuthorizationRow rowB = mock(TenantAuthorizationRow.class);
        when(rowB.getOrganizationId()).thenReturn(UUID.randomUUID());
        List<TenantAuthorizationRow> ambiguousRows = List.of(rowA, rowB);

        when(user.getId()).thenReturn(userId);
        when(userRepository.findActiveByEmail("platform.admin@demo.com"))
                .thenReturn(Optional.of(user));
        when(authorizationRepository.findPlatformAuthorizationByUserId(userId))
                .thenReturn(List.of());
        when(tenantAuthorizationRepository.findTenantAuthorizationByUserId(userId))
                .thenReturn(ambiguousRows);

        AuthMeService service = new AuthMeService(
                userRepository,
                authorizationRepository,
                tenantAuthorizationRepository,
                roleExtractor);

        assertThatThrownBy(() -> service.getCurrentUser(jwt))
                .isInstanceOf(AmbiguousTenantContextException.class);
    }

    @Test
    void returnsEmptyKeycloakRolesForUnexpectedClaimShape() {
        KeycloakRealmRoleExtractor roleExtractor = new KeycloakRealmRoleExtractor();
        Jwt jwt = jwt(Map.of(
                "email", "platform.admin@demo.com",
                "realm_access", "unexpected"
        ));

        assertThat(roleExtractor.extract(jwt)).isEmpty();
    }

    private Jwt jwt(Map<String, Object> claims) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue("test-token")
                .headers(headers -> headers.put("alg", "RS256"))
                .subject("keycloak-platform-admin-id")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claims(jwtClaims -> jwtClaims.putAll(claims))
                .build();
    }
}
