package com.aliozcan.airportops.iam_service.security;

import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.repository.PlatformAuthorizationRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformAuthorizationRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IamJwtAuthenticationConverterTests {

    private UserRepository userRepository;
    private PlatformAuthorizationRepository authorizationRepository;
    private IamJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authorizationRepository = mock(PlatformAuthorizationRepository.class);
        converter = new IamJwtAuthenticationConverter(
                userRepository,
                authorizationRepository);
    }

    @Test
    void convertsOnlyIamPermissionsToAuthorities() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user(userId);
        List<PlatformAuthorizationRow> rows = List.of(
                row("PLATFORM_ADMIN", "tenant:read"),
                row("PLATFORM_ADMIN", "platform:invitation:create"),
                row("PLATFORM_ADMIN", "tenant:read"),
                row("PLATFORM_ADMIN", null),
                row("PLATFORM_ADMIN", "   ")
        );
        when(userRepository.findActiveByEmail("platform.admin@demo.com"))
                .thenReturn(Optional.of(user));
        when(authorizationRepository.findPlatformAuthorizationByUserId(userId))
                .thenReturn(rows);

        AbstractAuthenticationToken authentication = converter.convert(jwt(
                "platform.admin@demo.com",
                true));

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authorities).containsExactlyInAnyOrder(
                "platform:invitation:create",
                "tenant:read"
        );
        assertThat(authorities)
                .doesNotContain("PLATFORM_ADMIN", "ROLE_PLATFORM_ADMIN");
        assertThat(authentication.getDetails())
                .isEqualTo(IamAuthenticationDetails.provisioned(userId));
    }

    @Test
    void marksMissingEmailAsUnprovisioned() {
        AbstractAuthenticationToken authentication = converter.convert(jwt(null, false));

        assertUnprovisioned(authentication);
        verifyNoInteractions(userRepository, authorizationRepository);
    }

    @Test
    void marksBlankEmailAsUnprovisioned() {
        AbstractAuthenticationToken authentication = converter.convert(jwt("   ", true));

        assertUnprovisioned(authentication);
        verifyNoInteractions(userRepository, authorizationRepository);
    }

    @Test
    void marksUnknownUserAsUnprovisioned() {
        when(userRepository.findActiveByEmail("missing@demo.com"))
                .thenReturn(Optional.empty());

        AbstractAuthenticationToken authentication = converter.convert(jwt(
                "  missing@demo.com  ",
                true));

        assertUnprovisioned(authentication);
        verifyNoInteractions(authorizationRepository);
    }

    @Test
    void keepsPermissionlessUserProvisionedWithEmptyAuthorities() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user(userId);
        when(userRepository.findActiveByEmail("permissionless@demo.com"))
                .thenReturn(Optional.of(user));
        when(authorizationRepository.findPlatformAuthorizationByUserId(userId))
                .thenReturn(List.of());

        AbstractAuthenticationToken authentication = converter.convert(jwt(
                "permissionless@demo.com",
                true));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities()).isEmpty();
        assertThat(authentication.getDetails())
                .isEqualTo(IamAuthenticationDetails.provisioned(userId));
    }

    @Test
    void propagatesRepositoryFailures() {
        DataAccessResourceFailureException failure =
                new DataAccessResourceFailureException("database unavailable");
        when(userRepository.findActiveByEmail("platform.admin@demo.com"))
                .thenThrow(failure);

        assertThatThrownBy(() -> converter.convert(jwt(
                "platform.admin@demo.com",
                true)))
                .isSameAs(failure);
    }

    private void assertUnprovisioned(AbstractAuthenticationToken authentication) {
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities()).isEmpty();
        assertThat(authentication.getDetails())
                .isEqualTo(IamAuthenticationDetails.unprovisioned());
    }

    private UserEntity user(UUID userId) {
        UserEntity user = mock(UserEntity.class);
        when(user.getId()).thenReturn(userId);
        return user;
    }

    private PlatformAuthorizationRow row(String roleCode, String permissionCode) {
        PlatformAuthorizationRow row = mock(PlatformAuthorizationRow.class);
        when(row.getRoleCode()).thenReturn(roleCode);
        when(row.getPermissionCode()).thenReturn(permissionCode);
        return row;
    }

    private Jwt jwt(String email, boolean includeEmail) {
        Instant issuedAt = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .headers(headers -> headers.put("alg", "RS256"))
                .subject("keycloak-user-id")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim("realm_access", Map.of(
                        "roles",
                        List.of("PLATFORM_ADMIN", "default-roles-airport-ops")
                ));
        if (includeEmail) {
            builder.claim("email", email);
        }
        return builder.build();
    }
}
