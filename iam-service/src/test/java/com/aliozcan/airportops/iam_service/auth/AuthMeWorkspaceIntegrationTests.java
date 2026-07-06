package com.aliozcan.airportops.iam_service.auth;

import com.aliozcan.airportops.iam_service.auth.dto.AuthMeResponse;
import com.aliozcan.airportops.iam_service.auth.dto.WorkspaceType;
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
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w2a.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w2a.test')",
        "DELETE FROM iam.platform_user_roles WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w2a.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w2a.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W2A %'",
        "INSERT INTO iam.users (email, full_name, status, auth_provider) VALUES "
                + "('tenant@w2a.test', 'Tenant User', 'ACTIVE', 'KEYCLOAK'), "
                + "('dual@w2a.test', 'Dual User', 'ACTIVE', 'KEYCLOAK'), "
                + "('none@w2a.test', 'No Workspace User', 'ACTIVE', 'KEYCLOAK'), "
                + "('inactive-org@w2a.test', 'Inactive Org User', 'ACTIVE', 'KEYCLOAK')",
        "INSERT INTO iam.organizations (name, status) VALUES "
                + "('W2A Tenant', 'ONBOARDING_INCOMPLETE'), "
                + "('W2A Dual', 'ACTIVE'), "
                + "('W2A Inactive', 'INACTIVE')",
        "INSERT INTO iam.organization_members (organization_id, user_id, status) "
                + "SELECT o.id, u.id, 'ACTIVE' FROM iam.users u "
                + "JOIN iam.organizations o ON "
                + "(u.email = 'tenant@w2a.test' AND o.name = 'W2A Tenant') OR "
                + "(u.email = 'dual@w2a.test' AND o.name = 'W2A Dual') OR "
                + "(u.email = 'inactive-org@w2a.test' AND o.name = 'W2A Inactive')",
        "INSERT INTO iam.member_roles (member_id, role_id) "
                + "SELECT m.id, r.id FROM iam.organization_members m "
                + "JOIN iam.users u ON u.id = m.user_id "
                + "JOIN iam.roles r ON r.code = 'AIRLINE_ADMIN' "
                + "WHERE u.email LIKE '%@w2a.test'",
        "INSERT INTO iam.platform_user_roles (user_id, role_id) "
                + "SELECT u.id, r.id FROM iam.users u "
                + "JOIN iam.roles r ON r.code = 'PLATFORM_ADMIN' "
                + "WHERE u.email = 'dual@w2a.test'"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w2a.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w2a.test')",
        "DELETE FROM iam.platform_user_roles WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w2a.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w2a.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W2A %'"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AuthMeWorkspaceIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsTenantWorkspaceAndRoleDerivedPermissions() {
        AuthMeResponse body = me(TestJwtDecoderConfig.TENANT_TOKEN).getBody();

        assertThat(body).isNotNull();
        assertThat(body.availableWorkspaces()).containsExactly(WorkspaceType.TENANT);
        assertThat(body.defaultWorkspace()).isEqualTo(WorkspaceType.TENANT);
        assertThat(body.iamRoles()).isEmpty();
        assertThat(body.permissions()).isEmpty();
        assertThat(body.tenantContext()).isNotNull();
        assertThat(body.tenantContext().organizationName()).isEqualTo("W2A Tenant");
        assertThat(body.tenantContext().roles()).containsExactly("AIRLINE_ADMIN");
        assertThat(body.tenantContext().permissions())
                .contains("member:invite", "flight:create", "audit:read");
    }

    @Test
    void defaultsToPlatformWhenBothWorkspacesAreAvailable() {
        AuthMeResponse body = me(TestJwtDecoderConfig.DUAL_WORKSPACE_TOKEN).getBody();

        assertThat(body).isNotNull();
        assertThat(body.availableWorkspaces())
                .containsExactly(WorkspaceType.PLATFORM, WorkspaceType.TENANT);
        assertThat(body.defaultWorkspace()).isEqualTo(WorkspaceType.PLATFORM);
        assertThat(body.tenantContext()).isNotNull();
    }

    @Test
    void returnsNullDefaultForUserWithoutAuthorizationContext() {
        AuthMeResponse body = me(TestJwtDecoderConfig.NO_WORKSPACE_TOKEN).getBody();

        assertThat(body).isNotNull();
        assertThat(body.availableWorkspaces()).isEmpty();
        assertThat(body.defaultWorkspace()).isNull();
        assertThat(body.tenantContext()).isNull();
    }

    @Test
    void inactiveOrganizationDoesNotProvideTenantWorkspace() {
        AuthMeResponse body = me(TestJwtDecoderConfig.INACTIVE_ORGANIZATION_TOKEN).getBody();

        assertThat(body).isNotNull();
        assertThat(body.availableWorkspaces()).isEmpty();
        assertThat(body.tenantContext()).isNull();
    }

    @Test
    void tenantPermissionsAreNotGrantedAsSpringAuthorities() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.TENANT_TOKEN);
        ResponseEntity<String> response = restTemplate.exchange(
                "/platform/authorization/probe",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("MISSING_PERMISSION");
    }

    private ResponseEntity<AuthMeResponse> me(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<AuthMeResponse> response = restTemplate.exchange(
                "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                AuthMeResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response;
    }
}
