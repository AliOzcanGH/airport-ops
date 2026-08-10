package com.aliozcan.airportops.iam_service.tenant.member;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.testsupport.MockAuditServiceConfig;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@Import({TestJwtDecoderConfig.class, MockAuditServiceConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TenantMemberRoleUpdateIntegrationTests {

    private static final String ORG_A = "W14 Org A";
    private static final String ORG_B = "W14 Org B";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockRestServiceServer mockAuditServiceServer;

    @BeforeEach
    void setUp() {
        cleanFixtures();

        jdbcTemplate.update(
                "INSERT INTO iam.organizations (name, status) VALUES (?, 'ACTIVE'), (?, 'ACTIVE')",
                ORG_A, ORG_B);

        jdbcTemplate.update(
                "INSERT INTO iam.users (email, full_name, status, auth_provider, preferred_language) VALUES "
                        + "('admin@w14.test', 'Org A Admin', 'ACTIVE', 'KEYCLOAK', 'TR'), "
                        + "('ops@w14.test', 'Org A Ops', 'ACTIVE', 'KEYCLOAK', 'TR'), "
                        + "('other-admin@w14.test', 'Org B Admin', 'ACTIVE', 'KEYCLOAK', 'TR')");

        assignMember("admin@w14.test", ORG_A, "AIRLINE_ADMIN");
        assignMember("ops@w14.test", ORG_A, "VIEWER");
        assignMember("other-admin@w14.test", ORG_B, "AIRLINE_ADMIN");
    }

    @AfterEach
    void tearDown() {
        cleanFixtures();
    }

    @Test
    void adminUpdatesMemberRoleAndTriggersAuditWrite() {
        UUID memberId = memberId("ops@w14.test");

        mockAuditServiceServer.expect(requestTo("http://mock-audit-service/internal/audit-logs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Service-Secret", "local-dev-internal-secret"))
                .andExpect(jsonPath("$.action").value("MEMBER_ROLE_UPDATED"))
                .andExpect(jsonPath("$.resourceType").value("MEMBER"))
                .andExpect(jsonPath("$.resourceId").value(memberId.toString()))
                .andExpect(jsonPath("$.metadata.previousRole").value("VIEWER"))
                .andExpect(jsonPath("$.metadata.newRole").value("OPS_USER"))
                .andRespond(withStatus(HttpStatus.CREATED));

        ResponseEntity<Map> response = updateRole(
                TestJwtDecoderConfig.W14_ADMIN_TOKEN, orgId(ORG_A), memberId, "OPS_USER", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("role")).isEqualTo("OPS_USER");

        String newRoleCode = jdbcTemplate.queryForObject(
                """
                        SELECT role_record.code FROM iam.member_roles member_role
                        JOIN iam.roles role_record ON role_record.id = member_role.role_id
                        WHERE member_role.member_id = ?
                        """,
                String.class, memberId);
        assertThat(newRoleCode).isEqualTo("OPS_USER");

        mockAuditServiceServer.verify();
    }

    @Test
    void nonAdminCannotUpdateRole() {
        UUID memberId = memberId("ops@w14.test");

        ResponseEntity<ErrorResponse> response = updateRole(
                TestJwtDecoderConfig.W14_OPS_TOKEN, orgId(ORG_A), memberId, "OPS_USER", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    @Test
    void rejectsPathOrgMismatch() {
        UUID memberId = memberId("ops@w14.test");

        ResponseEntity<ErrorResponse> response = updateRole(
                TestJwtDecoderConfig.W14_ADMIN_TOKEN, orgId(ORG_B), memberId, "OPS_USER", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().errorCode()).isEqualTo("TENANT_MISMATCH");
    }

    @Test
    void rejectsUnknownMember() {
        ResponseEntity<ErrorResponse> response = updateRole(
                TestJwtDecoderConfig.W14_ADMIN_TOKEN, orgId(ORG_A), UUID.randomUUID(), "OPS_USER",
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().errorCode()).isEqualTo("MEMBER_NOT_FOUND");
    }

    @Test
    void rejectsInvalidRoleValue() {
        UUID memberId = memberId("ops@w14.test");

        ResponseEntity<ErrorResponse> response = updateRole(
                TestJwtDecoderConfig.W14_ADMIN_TOKEN, orgId(ORG_A), memberId, "AIRLINE_ADMIN",
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void rejectsPlatformAdminAsNewRole() {
        UUID memberId = memberId("ops@w14.test");

        ResponseEntity<ErrorResponse> response = updateRole(
                TestJwtDecoderConfig.W14_ADMIN_TOKEN, orgId(ORG_A), memberId, "PLATFORM_ADMIN",
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void adminCannotChangeTheirOwnRole() {
        UUID adminMemberId = memberId("admin@w14.test");

        ResponseEntity<ErrorResponse> response = updateRole(
                TestJwtDecoderConfig.W14_ADMIN_TOKEN, orgId(ORG_A), adminMemberId, "OPS_USER",
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().errorCode()).isEqualTo("CANNOT_MODIFY_OWN_ROLE");

        String unchangedRole = jdbcTemplate.queryForObject(
                """
                        SELECT role_record.code FROM iam.member_roles member_role
                        JOIN iam.roles role_record ON role_record.id = member_role.role_id
                        WHERE member_role.member_id = ?
                        """,
                String.class, adminMemberId);
        assertThat(unchangedRole).isEqualTo("AIRLINE_ADMIN");
    }

    private void assignMember(String email, String organizationName, String roleCode) {
        UUID memberId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        INSERT INTO iam.organization_members (id, organization_id, user_id, status, joined_at)
                        SELECT ?, organization_record.id, user_record.id, 'ACTIVE', now()
                        FROM iam.organizations organization_record, iam.users user_record
                        WHERE organization_record.name = ? AND lower(user_record.email) = lower(?)
                        """,
                memberId, organizationName, email);
        jdbcTemplate.update(
                """
                        INSERT INTO iam.member_roles (member_id, role_id)
                        SELECT ?, role_record.id
                        FROM iam.roles role_record
                        WHERE role_record.code = ? AND role_record.scope = 'ORGANIZATION'
                        """,
                memberId, roleCode);
    }

    private UUID memberId(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM iam.organization_members WHERE user_id IN "
                        + "(SELECT id FROM iam.users WHERE lower(email) = lower(?))",
                UUID.class, email);
    }

    private UUID orgId(String organizationName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM iam.organizations WHERE name = ?", UUID.class, organizationName);
    }

    private <T> ResponseEntity<T> updateRole(
            String token, UUID orgId, UUID memberId, String role, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                "/organizations/" + orgId + "/members/" + memberId + "/role",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("role", role), headers),
                responseType);
    }

    private void cleanFixtures() {
        jdbcTemplate.update(
                "DELETE FROM iam.member_roles WHERE member_id IN "
                        + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                        + "(SELECT id FROM iam.users WHERE email LIKE '%@w14.test'))");
        jdbcTemplate.update(
                "DELETE FROM iam.organization_members WHERE user_id IN "
                        + "(SELECT id FROM iam.users WHERE email LIKE '%@w14.test')");
        jdbcTemplate.update("DELETE FROM iam.users WHERE email LIKE '%@w14.test'");
        jdbcTemplate.update("DELETE FROM iam.organizations WHERE name IN (?, ?)", ORG_A, ORG_B);
    }
}
