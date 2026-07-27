package com.aliozcan.airportops.iam_service.tenant.member;

import com.aliozcan.airportops.iam_service.keycloak.KeycloakProvisioningClient;
import com.aliozcan.airportops.iam_service.platform.invitation.InvitationTokenService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrganizationInvitationAcceptanceIntegrationTests {

    private static final String ENDPOINT = "/invitations/accept";
    private static final String TOKEN = "O".repeat(43);
    private static final String EMAIL = "new.joiner@w7.accept.test";
    private static final String ORGANIZATION_NAME = "W7 Accept Org";
    private static final String FULL_NAME = "New Joiner";
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InvitationTokenService invitationTokenService;

    @MockitoBean
    private KeycloakProvisioningClient keycloakProvisioningClient;

    @BeforeEach
    void setUp() {
        cleanFixtures();
        reset(keycloakProvisioningClient);
        when(keycloakProvisioningClient.createUser(anyString(), anyString(), anyString()))
                .thenReturn("keycloak-w7-subject");
    }

    @AfterEach
    void tearDown() {
        cleanFixtures();
    }

    @Test
    void acceptsOrganizationInvitationAndJoinsExistingOrgWithIntendedRole() throws Exception {
        UUID organizationId = insertActiveOrganization();
        insertOrganizationInvitation(organizationId, "OPS_USER");

        ResponseEntity<String> response = restTemplate.postForEntity(
                ENDPOINT,
                Map.of(
                        "token", TOKEN,
                        "fullName", FULL_NAME,
                        "password", PASSWORD,
                        "preferredLanguage", "TR"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("organizationName").asText()).isEqualTo(ORGANIZATION_NAME);
        assertThat(json.get("organizationStatus").asText()).isEqualTo("ACTIVE");

        Map<String, Object> organization = jdbcTemplate.queryForMap(
                "SELECT id, status FROM iam.organizations WHERE id = ?", organizationId);
        assertThat(organization.get("status")).isEqualTo("ACTIVE");

        String roleCode = jdbcTemplate.queryForObject(
                """
                        SELECT role_record.code
                        FROM iam.member_roles member_role
                        JOIN iam.organization_members member ON member.id = member_role.member_id
                        JOIN iam.users user_account ON user_account.id = member.user_id
                        JOIN iam.roles role_record ON role_record.id = member_role.role_id
                        WHERE lower(user_account.email) = lower(?)
                        """,
                String.class,
                EMAIL);
        assertThat(roleCode).isEqualTo("OPS_USER");

        Map<String, Object> invitation = jdbcTemplate.queryForMap(
                "SELECT status, organization_id FROM iam.invitations WHERE token_hash = ?",
                invitationTokenService.hash(TOKEN));
        assertThat(invitation.get("status")).isEqualTo("ACCEPTED");
        assertThat(invitation.get("organization_id")).isEqualTo(organizationId);
    }

    private UUID insertActiveOrganization() {
        UUID organizationId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO iam.organizations (id, name, status) VALUES (?, ?, 'ACTIVE')",
                organizationId, ORGANIZATION_NAME);
        return organizationId;
    }

    private void insertOrganizationInvitation(UUID organizationId, String intendedRole) {
        Instant now = Instant.now();
        int inserted = jdbcTemplate.update(
                """
                        INSERT INTO iam.invitations (
                            company_name, admin_email, token_hash, status,
                            created_by_user_id, organization_id, invitation_type, intended_role,
                            invitee_full_name, expires_at, created_at, updated_at
                        )
                        SELECT ?, ?, ?, 'PENDING', user_account.id, ?, 'ORGANIZATION', ?, ?, ?, ?, ?
                        FROM iam.users user_account
                        WHERE lower(user_account.email) = 'platform.admin@demo.com'
                        """,
                ORGANIZATION_NAME,
                EMAIL,
                invitationTokenService.hash(TOKEN),
                organizationId,
                intendedRole,
                FULL_NAME,
                Timestamp.from(now.plusSeconds(86_400)),
                Timestamp.from(now),
                Timestamp.from(now));
        assertThat(inserted).isEqualTo(1);
    }

    private void cleanFixtures() {
        jdbcTemplate.update(
                """
                        DELETE FROM iam.member_roles
                        WHERE member_id IN (
                            SELECT member.id
                            FROM iam.organization_members member
                            JOIN iam.users user_account ON user_account.id = member.user_id
                            WHERE lower(user_account.email) = lower(?)
                        )
                        """,
                EMAIL);
        jdbcTemplate.update(
                """
                        DELETE FROM iam.organization_members
                        WHERE user_id IN (SELECT id FROM iam.users WHERE lower(email) = lower(?))
                           OR organization_id IN (
                               SELECT id FROM iam.organizations WHERE lower(name) = lower(?)
                           )
                        """,
                EMAIL,
                ORGANIZATION_NAME);
        jdbcTemplate.update(
                "DELETE FROM iam.invitations WHERE lower(admin_email) = lower(?)", EMAIL);
        jdbcTemplate.update("DELETE FROM iam.users WHERE lower(email) = lower(?)", EMAIL);
        jdbcTemplate.update(
                "DELETE FROM iam.organizations WHERE lower(name) = lower(?)", ORGANIZATION_NAME);
    }
}
