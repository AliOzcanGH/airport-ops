package com.aliozcan.airportops.iam_service.platform.tenant;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.platform.tenant.dto.PlatformTenantDetailResponse;
import com.aliozcan.airportops.iam_service.platform.tenant.dto.PlatformTenantDirectoryResponse;
import com.aliozcan.airportops.iam_service.platform.tenant.dto.PlatformTenantMemberResponse;
import com.aliozcan.airportops.iam_service.platform.tenant.dto.PlatformTenantSummaryResponse;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlatformTenantDirectoryIntegrationTests {

    private static final String TENANTS_PATH = "/platform/tenants";
    private static final UUID ACTIVE_ORGANIZATION_ID =
            UUID.fromString("11111111-2222-4333-8444-555555555551");
    private static final UUID DELETED_ORGANIZATION_ID =
            UUID.fromString("11111111-2222-4333-8444-555555555553");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listsTenantOrganizationsForPlatformAdmin() {
        executeAll(TENANT_DIRECTORY_CLEANUP);
        executeAll(TENANT_DIRECTORY_SETUP);
        try {
            ResponseEntity<PlatformTenantDirectoryResponse> response = callTenants(
                    TestJwtDecoderConfig.VALID_TOKEN,
                    PlatformTenantDirectoryResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            List<PlatformTenantSummaryResponse> tenants =
                    response.getBody().tenants();
            PlatformTenantSummaryResponse activeTenant = tenants.stream()
                    .filter(tenant -> tenant.organizationName()
                            .equals("W4B Active Air Test"))
                    .findFirst()
                    .orElseThrow();
            assertThat(activeTenant.organizationStatus().name()).isEqualTo("ACTIVE");
            assertThat(activeTenant.memberCount()).isEqualTo(2);
            assertThat(activeTenant.primaryAdminEmail())
                    .isEqualTo("w4b.airline.admin@test.com");
            assertThat(activeTenant.createdAt()).isNotNull();

            assertThat(tenants)
                    .extracting(PlatformTenantSummaryResponse::organizationName)
                    .contains("W4B Onboarding Air Test")
                    .doesNotContain("W4B Deleted Air Test");
        } finally {
            executeAll(TENANT_DIRECTORY_CLEANUP);
        }
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(TENANTS_PATH, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsTenantOnlyUser() {
        executeAll(TENANT_USER_CLEANUP);
        executeAll(TENANT_USER_SETUP);
        try {
            ResponseEntity<ErrorResponse> response = callTenants(
                    TestJwtDecoderConfig.TENANT_TOKEN,
                    ErrorResponse.class);

            assertForbidden(response, "MISSING_PERMISSION");
        } finally {
            executeAll(TENANT_USER_CLEANUP);
        }
    }

    @Test
    @Sql(statements = {
            "DELETE FROM iam.platform_user_roles WHERE user_id IN "
                    + "(SELECT id FROM iam.users WHERE lower(email) = "
                    + "lower('k4.permissionless@integration.test'))",
            "DELETE FROM iam.users WHERE lower(email) = "
                    + "lower('k4.permissionless@integration.test')",
            "INSERT INTO iam.users (email, password_hash, full_name, status) VALUES "
                    + "('k4.permissionless@integration.test', 'not-used', "
                    + "'K4 Permissionless Test User', 'ACTIVE')"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = {
            "DELETE FROM iam.platform_user_roles WHERE user_id IN "
                    + "(SELECT id FROM iam.users WHERE lower(email) = "
                    + "lower('k4.permissionless@integration.test'))",
            "DELETE FROM iam.users WHERE lower(email) = "
                    + "lower('k4.permissionless@integration.test')"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void rejectsPlatformUserWithoutTenantReadPermission() {
        ResponseEntity<ErrorResponse> response = callTenants(
                TestJwtDecoderConfig.PERMISSIONLESS_TOKEN,
                ErrorResponse.class);

        assertForbidden(response, "MISSING_PERMISSION");
    }

    @Test
    void returnsTenantDetailForPlatformAdmin() {
        executeAll(TENANT_DIRECTORY_CLEANUP);
        executeAll(TENANT_DIRECTORY_SETUP);
        try {
            ResponseEntity<PlatformTenantDetailResponse> response =
                    callTenantDetail(
                            TestJwtDecoderConfig.VALID_TOKEN,
                            ACTIVE_ORGANIZATION_ID,
                            PlatformTenantDetailResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            PlatformTenantDetailResponse body = response.getBody();
            assertThat(body.organizationId()).isEqualTo(ACTIVE_ORGANIZATION_ID);
            assertThat(body.organizationName()).isEqualTo("W4B Active Air Test");
            assertThat(body.organizationStatus().name()).isEqualTo("ACTIVE");
            assertThat(body.memberCount()).isEqualTo(2);
            assertThat(body.primaryAdminEmail())
                    .isEqualTo("w4b.airline.admin@test.com");
            assertThat(body.members()).hasSize(2);

            PlatformTenantMemberResponse admin = body.members()
                    .stream()
                    .filter(member -> member.email()
                            .equals("w4b.airline.admin@test.com"))
                    .findFirst()
                    .orElseThrow();
            assertThat(admin.fullName()).isEqualTo("W4B Airline Admin");
            assertThat(admin.memberStatus().name()).isEqualTo("ACTIVE");
            assertThat(admin.roles())
                    .containsExactly("AIRLINE_ADMIN", "OPS_USER");
            assertThat(admin.joinedAt()).isNotNull();

            assertThat(body.members())
                    .extracting(PlatformTenantMemberResponse::email)
                    .contains("w4b.ops.user@test.com")
                    .doesNotContain("w4b.inactive.member@test.com");
        } finally {
            executeAll(TENANT_DIRECTORY_CLEANUP);
        }
    }

    @Test
    void rejectsTenantDetailRequestWithoutBearerToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                TENANTS_PATH + "/" + ACTIVE_ORGANIZATION_ID,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsTenantOnlyUserForTenantDetail() {
        executeAll(TENANT_USER_CLEANUP);
        executeAll(TENANT_USER_SETUP);
        try {
            ResponseEntity<ErrorResponse> response = callTenantDetail(
                    TestJwtDecoderConfig.TENANT_TOKEN,
                    ACTIVE_ORGANIZATION_ID,
                    ErrorResponse.class);

            assertForbidden(response, "MISSING_PERMISSION");
        } finally {
            executeAll(TENANT_USER_CLEANUP);
        }
    }

    @Test
    @Sql(statements = {
            "DELETE FROM iam.platform_user_roles WHERE user_id IN "
                    + "(SELECT id FROM iam.users WHERE lower(email) = "
                    + "lower('k4.permissionless@integration.test'))",
            "DELETE FROM iam.users WHERE lower(email) = "
                    + "lower('k4.permissionless@integration.test')",
            "INSERT INTO iam.users (email, password_hash, full_name, status) VALUES "
                    + "('k4.permissionless@integration.test', 'not-used', "
                    + "'K4 Permissionless Test User', 'ACTIVE')"
    }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = {
            "DELETE FROM iam.platform_user_roles WHERE user_id IN "
                    + "(SELECT id FROM iam.users WHERE lower(email) = "
                    + "lower('k4.permissionless@integration.test'))",
            "DELETE FROM iam.users WHERE lower(email) = "
                    + "lower('k4.permissionless@integration.test')"
    }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void rejectsPlatformUserWithoutTenantReadPermissionForTenantDetail() {
        ResponseEntity<ErrorResponse> response = callTenantDetail(
                TestJwtDecoderConfig.PERMISSIONLESS_TOKEN,
                ACTIVE_ORGANIZATION_ID,
                ErrorResponse.class);

        assertForbidden(response, "MISSING_PERMISSION");
    }

    @Test
    void returnsNotFoundForUnknownTenantDetail() {
        ResponseEntity<ErrorResponse> response = callTenantDetail(
                TestJwtDecoderConfig.VALID_TOKEN,
                UUID.fromString("99999999-2222-4333-8444-555555555551"),
                ErrorResponse.class);

        assertNotFound(response);
    }

    @Test
    void returnsNotFoundForSoftDeletedTenantDetail() {
        executeAll(TENANT_DIRECTORY_CLEANUP);
        executeAll(TENANT_DIRECTORY_SETUP);
        try {
            ResponseEntity<ErrorResponse> response = callTenantDetail(
                    TestJwtDecoderConfig.VALID_TOKEN,
                    DELETED_ORGANIZATION_ID,
                    ErrorResponse.class);

            assertNotFound(response);
        } finally {
            executeAll(TENANT_DIRECTORY_CLEANUP);
        }
    }

    private <T> ResponseEntity<T> callTenants(
            String token,
            Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                TENANTS_PATH,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                responseType);
    }

    private <T> ResponseEntity<T> callTenantDetail(
            String token,
            UUID organizationId,
            Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                TENANTS_PATH + "/" + organizationId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                responseType);
    }

    private void assertForbidden(
            ResponseEntity<ErrorResponse> response,
            String errorCode) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status())
                .isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getBody().error()).isEqualTo("FORBIDDEN");
        assertThat(response.getBody().errorCode()).isEqualTo(errorCode);
        assertThat(response.getBody().path()).startsWith(TENANTS_PATH);
    }

    private void assertNotFound(ResponseEntity<ErrorResponse> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().errorCode()).isEqualTo("TENANT_NOT_FOUND");
        assertThat(response.getBody().message())
                .isEqualTo("Tenant organization not found");
    }

    private void executeAll(String[] statements) {
        for (String statement : statements) {
            jdbcTemplate.execute(statement);
        }
    }

    private static final String[] TENANT_DIRECTORY_CLEANUP = {
            "DELETE FROM iam.member_roles WHERE member_id IN "
                    + "(SELECT id FROM iam.organization_members WHERE "
                    + "organization_id IN ("
                    + "'11111111-2222-4333-8444-555555555551',"
                    + "'11111111-2222-4333-8444-555555555552',"
                    + "'11111111-2222-4333-8444-555555555553'))",
            "DELETE FROM iam.organization_members WHERE organization_id IN ("
                    + "'11111111-2222-4333-8444-555555555551',"
                    + "'11111111-2222-4333-8444-555555555552',"
                    + "'11111111-2222-4333-8444-555555555553')",
            "DELETE FROM iam.users WHERE lower(email) IN ("
                    + "lower('w4b.airline.admin@test.com'),"
                    + "lower('w4b.ops.user@test.com'),"
                    + "lower('w4b.inactive.member@test.com'))",
            "DELETE FROM iam.organizations WHERE id IN ("
                    + "'11111111-2222-4333-8444-555555555551',"
                    + "'11111111-2222-4333-8444-555555555552',"
                    + "'11111111-2222-4333-8444-555555555553')"
    };

    private static final String[] TENANT_DIRECTORY_SETUP = {
            "DELETE FROM iam.member_roles WHERE member_id IN "
                    + "(SELECT id FROM iam.organization_members WHERE "
                    + "organization_id IN ("
                    + "'11111111-2222-4333-8444-555555555551',"
                    + "'11111111-2222-4333-8444-555555555552',"
                    + "'11111111-2222-4333-8444-555555555553'))",
            "DELETE FROM iam.organization_members WHERE organization_id IN ("
                    + "'11111111-2222-4333-8444-555555555551',"
                    + "'11111111-2222-4333-8444-555555555552',"
                    + "'11111111-2222-4333-8444-555555555553')",
            "DELETE FROM iam.users WHERE lower(email) IN ("
                    + "lower('w4b.airline.admin@test.com'),"
                    + "lower('w4b.ops.user@test.com'),"
                    + "lower('w4b.inactive.member@test.com'))",
            "DELETE FROM iam.organizations WHERE id IN ("
                    + "'11111111-2222-4333-8444-555555555551',"
                    + "'11111111-2222-4333-8444-555555555552',"
                    + "'11111111-2222-4333-8444-555555555553')",
            "INSERT INTO iam.organizations "
                    + "(id, name, status, created_at, updated_at, deleted_at) VALUES "
                    + "('11111111-2222-4333-8444-555555555551', "
                    + "'W4B Active Air Test', 'ACTIVE', "
                    + "'2026-07-13T10:00:00Z', '2026-07-13T10:00:00Z', NULL)",
            "INSERT INTO iam.organizations "
                    + "(id, name, status, created_at, updated_at, deleted_at) VALUES "
                    + "('11111111-2222-4333-8444-555555555552', "
                    + "'W4B Onboarding Air Test', 'ONBOARDING_INCOMPLETE', "
                    + "'2026-07-12T10:00:00Z', '2026-07-12T10:00:00Z', NULL)",
            "INSERT INTO iam.organizations "
                    + "(id, name, status, created_at, updated_at, deleted_at) VALUES "
                    + "('11111111-2222-4333-8444-555555555553', "
                    + "'W4B Deleted Air Test', 'INACTIVE', "
                    + "'2026-07-11T10:00:00Z', '2026-07-11T10:00:00Z', "
                    + "'2026-07-11T11:00:00Z')",
            "INSERT INTO iam.users "
                    + "(id, email, password_hash, full_name, status) VALUES "
                    + "('22222222-2222-4333-8444-555555555551', "
                    + "'w4b.airline.admin@test.com', 'not-used', "
                    + "'W4B Airline Admin', 'ACTIVE')",
            "INSERT INTO iam.users "
                    + "(id, email, password_hash, full_name, status) VALUES "
                    + "('22222222-2222-4333-8444-555555555552', "
                    + "'w4b.ops.user@test.com', 'not-used', "
                    + "'W4B Ops User', 'ACTIVE')",
            "INSERT INTO iam.users "
                    + "(id, email, password_hash, full_name, status, deleted_at) VALUES "
                    + "('22222222-2222-4333-8444-555555555553', "
                    + "'w4b.inactive.member@test.com', 'not-used', "
                    + "'W4B Inactive Member', 'INACTIVE', "
                    + "'2026-07-13T12:00:00Z')",
            "INSERT INTO iam.organization_members "
                    + "(id, organization_id, user_id, status, joined_at) VALUES "
                    + "('33333333-2222-4333-8444-555555555551', "
                    + "'11111111-2222-4333-8444-555555555551', "
                    + "'22222222-2222-4333-8444-555555555551', 'ACTIVE', now())",
            "INSERT INTO iam.organization_members "
                    + "(id, organization_id, user_id, status, joined_at) VALUES "
                    + "('33333333-2222-4333-8444-555555555552', "
                    + "'11111111-2222-4333-8444-555555555551', "
                    + "'22222222-2222-4333-8444-555555555552', 'ACTIVE', now())",
            "INSERT INTO iam.organization_members "
                    + "(id, organization_id, user_id, status, joined_at, deleted_at) VALUES "
                    + "('33333333-2222-4333-8444-555555555553', "
                    + "'11111111-2222-4333-8444-555555555551', "
                    + "'22222222-2222-4333-8444-555555555553', 'INACTIVE', "
                    + "now(), '2026-07-13T12:00:00Z')",
            "INSERT INTO iam.member_roles (member_id, role_id) "
                    + "SELECT '33333333-2222-4333-8444-555555555551', id "
                    + "FROM iam.roles WHERE code = 'AIRLINE_ADMIN'",
            "INSERT INTO iam.member_roles (member_id, role_id) "
                    + "SELECT '33333333-2222-4333-8444-555555555551', id "
                    + "FROM iam.roles WHERE code = 'OPS_USER'",
            "INSERT INTO iam.member_roles (member_id, role_id) "
                    + "SELECT '33333333-2222-4333-8444-555555555552', id "
                    + "FROM iam.roles WHERE code = 'OPS_USER'"
    };

    private static final String[] TENANT_USER_CLEANUP = {
            "DELETE FROM iam.member_roles WHERE member_id IN "
                    + "(SELECT id FROM iam.organization_members WHERE "
                    + "user_id IN (SELECT id FROM iam.users WHERE lower(email) = "
                    + "lower('tenant@w2a.test')))",
            "DELETE FROM iam.organization_members WHERE user_id IN "
                    + "(SELECT id FROM iam.users WHERE lower(email) = "
                    + "lower('tenant@w2a.test'))",
            "DELETE FROM iam.users WHERE lower(email) = lower('tenant@w2a.test')",
            "DELETE FROM iam.organizations WHERE name = 'W4B Tenant Token Org'"
    };

    private static final String[] TENANT_USER_SETUP = {
            "DELETE FROM iam.member_roles WHERE member_id IN "
                    + "(SELECT id FROM iam.organization_members WHERE "
                    + "user_id IN (SELECT id FROM iam.users WHERE lower(email) = "
                    + "lower('tenant@w2a.test')))",
            "DELETE FROM iam.organization_members WHERE user_id IN "
                    + "(SELECT id FROM iam.users WHERE lower(email) = "
                    + "lower('tenant@w2a.test'))",
            "DELETE FROM iam.users WHERE lower(email) = lower('tenant@w2a.test')",
            "DELETE FROM iam.organizations WHERE name = 'W4B Tenant Token Org'",
            "INSERT INTO iam.users "
                    + "(id, email, password_hash, full_name, status) VALUES "
                    + "('44444444-2222-4333-8444-555555555551', "
                    + "'tenant@w2a.test', 'not-used', 'W4B Tenant User', 'ACTIVE')",
            "INSERT INTO iam.organizations "
                    + "(id, name, status) VALUES "
                    + "('44444444-2222-4333-8444-555555555552', "
                    + "'W4B Tenant Token Org', 'ACTIVE')",
            "INSERT INTO iam.organization_members "
                    + "(id, organization_id, user_id, status, joined_at) VALUES "
                    + "('44444444-2222-4333-8444-555555555553', "
                    + "'44444444-2222-4333-8444-555555555552', "
                    + "'44444444-2222-4333-8444-555555555551', 'ACTIVE', now())",
            "INSERT INTO iam.member_roles (member_id, role_id) "
                    + "SELECT '44444444-2222-4333-8444-555555555553', id "
                    + "FROM iam.roles WHERE code = 'AIRLINE_ADMIN'"
    };
}
