package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.UserStatus;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformAuthorizationRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IamRepositoryIntegrationTests {

    private static final String PLATFORM_ADMIN_EMAIL = "platform.admin@demo.com";
    private static final Set<String> PLATFORM_ADMIN_PERMISSIONS = Set.of(
            "platform:invitation:create",
            "tenant:read",
            "tenant:manage"
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformAuthorizationRepository platformAuthorizationRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void findsActivePlatformAdminByEmail() {
        Optional<UserEntity> result = userRepository.findActiveByEmail(PLATFORM_ADMIN_EMAIL);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.orElseThrow().getDeletedAt()).isNull();
    }

    @Test
    void emailLookupIsCaseInsensitive() {
        UserEntity lowercaseResult = userRepository.findActiveByEmail(PLATFORM_ADMIN_EMAIL)
                .orElseThrow();
        UserEntity uppercaseResult = userRepository.findActiveByEmail("PLATFORM.ADMIN@DEMO.COM")
                .orElseThrow();

        assertThat(uppercaseResult.getId()).isEqualTo(lowercaseResult.getId());
    }

    @Test
    void returnsEmptyForUnknownEmail() {
        Optional<UserEntity> result = userRepository.findActiveByEmail("missing.user@demo.com");

        assertThat(result).isEmpty();
    }

    @Test
    void loadsOnlyPlatformAdminAuthorization() {
        UserEntity platformAdmin = userRepository.findActiveByEmail(PLATFORM_ADMIN_EMAIL)
                .orElseThrow();

        List<PlatformAuthorizationRow> rows =
                platformAuthorizationRepository.findPlatformAuthorizationByUserId(
                        platformAdmin.getId());

        Set<String> roleCodes = rows.stream()
                .map(PlatformAuthorizationRow::getRoleCode)
                .collect(Collectors.toSet());
        Set<String> permissionCodes = rows.stream()
                .map(PlatformAuthorizationRow::getPermissionCode)
                .filter(permissionCode -> permissionCode != null)
                .collect(Collectors.toSet());

        assertThat(roleCodes).containsExactly("PLATFORM_ADMIN");
        assertThat(permissionCodes).containsExactlyInAnyOrderElementsOf(
                PLATFORM_ADMIN_PERMISSIONS);
        assertThat(rows).hasSize(permissionCodes.size());
        assertThat(permissionCodes)
                .noneMatch(permissionCode -> permissionCode.startsWith("member:")
                        || permissionCode.startsWith("station:")
                        || permissionCode.startsWith("gate:")
                        || permissionCode.startsWith("flight:")
                        || permissionCode.startsWith("task:")
                        || permissionCode.startsWith("report:")
                        || permissionCode.startsWith("audit:"));
    }

    @Test
    void actuatorHealthRemainsUp() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
