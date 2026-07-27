package com.aliozcan.airportops.iam_service.tenant.member;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TenantMemberIntegrationTests {

    private static final String ORG_A = "W7 Org A";
    private static final String ORG_B = "W7 Org B";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanFixtures();

        jdbcTemplate.update(
                "INSERT INTO iam.organizations (name, status) VALUES (?, 'ACTIVE'), (?, 'ACTIVE')",
                ORG_A, ORG_B);

        jdbcTemplate.update(
                "INSERT INTO iam.users (email, full_name, status, auth_provider, preferred_language) VALUES "
                        + "('admin@w7.test', 'Org A Admin', 'ACTIVE', 'KEYCLOAK', 'TR'), "
                        + "('ops@w7.test', 'Org A Ops', 'ACTIVE', 'KEYCLOAK', 'TR'), "
                        + "('existing-member@w7.test', 'Org A Existing Member', 'ACTIVE', 'KEYCLOAK', 'TR'), "
                        + "('other-admin@w7.test', 'Org B Admin', 'ACTIVE', 'KEYCLOAK', 'TR')");

        assignMember("admin@w7.test", ORG_A, "AIRLINE_ADMIN");
        assignMember("ops@w7.test", ORG_A, "OPS_USER");
        assignMember("existing-member@w7.test", ORG_A, "VIEWER");
        assignMember("other-admin@w7.test", ORG_B, "AIRLINE_ADMIN");
    }

    @AfterEach
    void tearDown() {
        cleanFixtures();
    }

    @Test
    void returnsUnauthorizedWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                membersEndpoint(orgId(ORG_A)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void nonAdminCannotInvite() {
        ResponseEntity<ErrorResponse> response = invite(
                TestJwtDecoderConfig.W7_OPS_TOKEN,
                orgId(ORG_A),
                "new.member@w7.test",
                "New Member",
                "OPS_USER",
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    @Test
    void adminInvitesOpsUserSuccessfully() throws Exception {
        ResponseEntity<String> response = invite(
                TestJwtDecoderConfig.W7_ADMIN_TOKEN,
                orgId(ORG_A),
                "new.member@w7.test",
                "New Member",
                "OPS_USER",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("email").asText()).isEqualTo("new.member@w7.test");
        assertThat(json.get("intendedRole").asText()).isEqualTo("OPS_USER");
        assertThat(json.get("status").asText()).isEqualTo("PENDING");

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT invitation_type, intended_role, organization_id "
                        + "FROM iam.invitations WHERE lower(admin_email) = 'new.member@w7.test'");
        assertThat(row.get("invitation_type")).isEqualTo("ORGANIZATION");
        assertThat(row.get("intended_role")).isEqualTo("OPS_USER");
        assertThat(row.get("organization_id")).isEqualTo(orgId(ORG_A));
    }

    @Test
    void cannotInviteAsAirlineAdmin() {
        ResponseEntity<ErrorResponse> response = invite(
                TestJwtDecoderConfig.W7_ADMIN_TOKEN,
                orgId(ORG_A),
                "new.member@w7.test",
                "New Member",
                "AIRLINE_ADMIN",
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void rejectsPathOrgMismatch() {
        ResponseEntity<ErrorResponse> response = invite(
                TestJwtDecoderConfig.W7_ADMIN_TOKEN,
                orgId(ORG_B),
                "new.member@w7.test",
                "New Member",
                "OPS_USER",
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("TENANT_MISMATCH");
    }

    @Test
    void rejectsDuplicatePendingInvitation() {
        invite(TestJwtDecoderConfig.W7_ADMIN_TOKEN, orgId(ORG_A),
                "new.member@w7.test", "New Member", "OPS_USER", String.class);

        ResponseEntity<ErrorResponse> response = invite(
                TestJwtDecoderConfig.W7_ADMIN_TOKEN,
                orgId(ORG_A),
                "new.member@w7.test",
                "New Member",
                "VIEWER",
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("PENDING_INVITATION_EXISTS");
    }

    @Test
    void rejectsAlreadyActiveMemberEmail() {
        ResponseEntity<ErrorResponse> response = invite(
                TestJwtDecoderConfig.W7_ADMIN_TOKEN,
                orgId(ORG_A),
                "existing-member@w7.test",
                "Existing Member",
                "OPS_USER",
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("DUPLICATE_RESOURCE");
    }

    @Test
    void listsOnlyOwnOrganizationMembers() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                membersEndpoint(orgId(ORG_A)),
                HttpMethod.GET,
                authenticated(TestJwtDecoderConfig.W7_ADMIN_TOKEN),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode members = objectMapper.readTree(response.getBody());
        assertThat(members.isArray()).isTrue();
        assertThat(members.size()).isEqualTo(3);
        for (JsonNode member : members) {
            assertThat(member.get("email").asText()).endsWith("@w7.test");
        }
    }

    @Test
    void rejectsMemberListForNonAdmin() {
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                membersEndpoint(orgId(ORG_A)),
                HttpMethod.GET,
                authenticated(TestJwtDecoderConfig.W7_OPS_TOKEN),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    @Test
    void rejectsMemberListForCrossTenantOrgId() {
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                membersEndpoint(orgId(ORG_B)),
                HttpMethod.GET,
                authenticated(TestJwtDecoderConfig.W7_ADMIN_TOKEN),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("TENANT_MISMATCH");
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

    private UUID orgId(String organizationName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM iam.organizations WHERE name = ?",
                UUID.class,
                organizationName);
    }

    private String membersEndpoint(UUID orgId) {
        return "/organizations/" + orgId + "/members";
    }

    private String invitationsEndpoint(UUID orgId) {
        return "/organizations/" + orgId + "/invitations";
    }

    private <T> ResponseEntity<T> invite(
            String token,
            UUID orgId,
            String email,
            String fullName,
            String intendedRole,
            Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of(
                "email", email,
                "fullName", fullName,
                "intendedRole", intendedRole);
        return restTemplate.exchange(
                invitationsEndpoint(orgId),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                responseType);
    }

    private HttpEntity<Void> authenticated(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private void cleanFixtures() {
        jdbcTemplate.update(
                "DELETE FROM iam.member_roles WHERE member_id IN "
                        + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                        + "(SELECT id FROM iam.users WHERE email LIKE '%@w7.test'))");
        jdbcTemplate.update(
                "DELETE FROM iam.organization_members WHERE user_id IN "
                        + "(SELECT id FROM iam.users WHERE email LIKE '%@w7.test')");
        jdbcTemplate.update("DELETE FROM iam.invitations WHERE lower(admin_email) LIKE '%@w7.test'");
        jdbcTemplate.update("DELETE FROM iam.users WHERE email LIKE '%@w7.test'");
        jdbcTemplate.update("DELETE FROM iam.organizations WHERE name IN (?, ?)", ORG_A, ORG_B);
    }
}
