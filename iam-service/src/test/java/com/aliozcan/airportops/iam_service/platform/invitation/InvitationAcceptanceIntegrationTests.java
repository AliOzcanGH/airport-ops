package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.domain.model.enums.UserStatus;
import com.aliozcan.airportops.iam_service.keycloak.KeycloakProvisioningClient;
import com.aliozcan.airportops.iam_service.keycloak.KeycloakProvisioningException;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InvitationAcceptanceIntegrationTests {

    private static final String ENDPOINT = "/invitations/accept";
    private static final String TOKEN = "K".repeat(43);
    private static final String EMAIL = "admin@k8.integration.test";
    private static final String ORGANIZATION_NAME = "K8 Pegasus Airlines";
    private static final String FULL_NAME = "Airline Admin";
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
        when(keycloakProvisioningClient.createUser(
                anyString(),
                anyString(),
                anyString()))
                .thenReturn("keycloak-k8-subject");
    }

    @AfterEach
    void tearDown() {
        cleanFixtures();
    }

    @Test
    void acceptsInvitationAfterIamCommitAndReturnsReady() throws Exception {
        insertInvitation(TOKEN, "PENDING", Instant.now().plusSeconds(86_400));
        AtomicBoolean keycloakCallHadTransaction = new AtomicBoolean(true);
        AtomicReference<String> committedInvitationStatus = new AtomicReference<>();
        AtomicReference<String> committedUserStatus = new AtomicReference<>();
        when(keycloakProvisioningClient.createUser(EMAIL, FULL_NAME, PASSWORD))
                .thenAnswer(invocation -> {
                    keycloakCallHadTransaction.set(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive());
                    committedInvitationStatus.set(jdbcTemplate.queryForObject(
                            "SELECT status FROM iam.invitations WHERE token_hash = ?",
                            String.class,
                            invitationTokenService.hash(TOKEN)));
                    committedUserStatus.set(jdbcTemplate.queryForObject(
                            "SELECT status FROM iam.users WHERE lower(email) = lower(?)",
                            String.class,
                            EMAIL));
                    return "keycloak-k8-subject";
                });

        ResponseEntity<String> response = restTemplate.postForEntity(
                ENDPOINT,
                requestWithIgnoredIdentityFields(),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(keycloakCallHadTransaction.get()).isFalse();
        assertThat(committedInvitationStatus).hasValue("ACCEPTED");
        assertThat(committedUserStatus).hasValue("PROVISIONING");
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("email").asText()).isEqualTo(EMAIL);
        assertThat(json.get("organizationName").asText())
                .isEqualTo(ORGANIZATION_NAME);
        assertThat(json.get("organizationStatus").asText())
                .isEqualTo("ONBOARDING_INCOMPLETE");
        assertThat(json.get("userStatus").asText()).isEqualTo("ACTIVE");
        assertThat(json.get("provisioningStatus").asText()).isEqualTo("READY");
        assertThat(response.getBody())
                .doesNotContain(TOKEN)
                .doesNotContain(PASSWORD)
                .doesNotContain("tokenHash")
                .doesNotContain("userId")
                .doesNotContain("organizationId");

        Map<String, Object> user = jdbcTemplate.queryForMap(
                """
                        SELECT full_name, status, auth_provider, password_hash,
                               keycloak_user_id
                        FROM iam.users
                        WHERE lower(email) = lower(?)
                        """,
                EMAIL);
        assertThat(user.get("full_name")).isEqualTo(FULL_NAME);
        assertThat(user.get("status")).isEqualTo("ACTIVE");
        assertThat(user.get("auth_provider")).isEqualTo("KEYCLOAK");
        assertThat(user.get("password_hash")).isNull();
        assertThat(user.get("keycloak_user_id"))
                .isEqualTo("keycloak-k8-subject");

        assertProvisionedBusinessState();
        verify(keycloakProvisioningClient).createUser(EMAIL, FULL_NAME, PASSWORD);
    }

    @Test
    void returnsAcceptedWhenKeycloakSyncFails() {
        insertInvitation(TOKEN, "PENDING", Instant.now().plusSeconds(86_400));
        when(keycloakProvisioningClient.createUser(EMAIL, FULL_NAME, PASSWORD))
                .thenThrow(new KeycloakProvisioningException(
                        "Keycloak unavailable",
                        false));

        ResponseEntity<String> response = accept(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody())
                .contains("\"userStatus\":\"KEYCLOAK_SYNC_FAILED\"")
                .contains("\"provisioningStatus\":\"LOGIN_SETUP_PENDING\"");
        Map<String, Object> user = userState();
        assertThat(user.get("status")).isEqualTo("KEYCLOAK_SYNC_FAILED");
        assertThat(user.get("keycloak_user_id")).isNull();
        assertThat(invitationStatus()).isEqualTo("ACCEPTED");
        assertProvisionedBusinessState();
    }

    @Test
    void doesNotAutoLinkDuplicateKeycloakIdentity() {
        insertInvitation(TOKEN, "PENDING", Instant.now().plusSeconds(86_400));
        when(keycloakProvisioningClient.createUser(EMAIL, FULL_NAME, PASSWORD))
                .thenThrow(new KeycloakProvisioningException(
                        "duplicate identity",
                        true));

        ResponseEntity<String> response = accept(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(userState().get("status"))
                .isEqualTo("KEYCLOAK_SYNC_FAILED");
        assertThat(userState().get("keycloak_user_id")).isNull();
    }

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    void rejectsEveryNonDeletedExistingUserStatus(UserStatus status) {
        insertInvitation(TOKEN, "PENDING", Instant.now().plusSeconds(86_400));
        insertExistingUser(status);

        ResponseEntity<ErrorResponse> response = accept(ErrorResponse.class);

        assertError(response, HttpStatus.CONFLICT, "IAM_USER_ALREADY_EXISTS");
        assertThat(invitationStatus()).isEqualTo("PENDING");
        assertThat(countOrganizations()).isZero();
        verifyNoInteractions(keycloakProvisioningClient);
    }

    @Test
    void rejectsExistingOrganizationAndRollsBackUserCreation() {
        insertInvitation(TOKEN, "PENDING", Instant.now().plusSeconds(86_400));
        jdbcTemplate.update(
                """
                        INSERT INTO iam.organizations (name, status)
                        VALUES (?, 'ACTIVE')
                        """,
                ORGANIZATION_NAME);

        ResponseEntity<ErrorResponse> response = accept(ErrorResponse.class);

        assertError(response, HttpStatus.CONFLICT, "ORGANIZATION_ALREADY_EXISTS");
        assertThat(invitationStatus()).isEqualTo("PENDING");
        assertThat(countUsers()).isZero();
        verifyNoInteractions(keycloakProvisioningClient);
    }

    @Test
    void rejectsReusedAcceptedInvitation() {
        insertInvitation(TOKEN, "PENDING", Instant.now().plusSeconds(86_400));
        assertThat(accept(String.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ErrorResponse> second = accept(ErrorResponse.class);

        assertError(second, HttpStatus.CONFLICT, "INVITATION_ALREADY_USED");
        verify(keycloakProvisioningClient).createUser(EMAIL, FULL_NAME, PASSWORD);
    }

    @Test
    void serializesConcurrentAcceptRequestsForTheSameToken() throws Exception {
        insertInvitation(TOKEN, "PENDING", Instant.now().plusSeconds(86_400));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<ResponseEntity<String>> first = executor.submit(() -> {
                start.await();
                return accept(String.class);
            });
            Future<ResponseEntity<String>> second = executor.submit(() -> {
                start.await();
                return accept(String.class);
            });
            start.countDown();

            List<HttpStatus> statuses = List.of(
                    HttpStatus.valueOf(first.get(15, TimeUnit.SECONDS)
                            .getStatusCode().value()),
                    HttpStatus.valueOf(second.get(15, TimeUnit.SECONDS)
                            .getStatusCode().value()));

            assertThat(statuses)
                    .containsExactlyInAnyOrder(
                            HttpStatus.CREATED,
                            HttpStatus.CONFLICT);
            assertThat(invitationStatus()).isEqualTo("ACCEPTED");
            assertThat(countUsers()).isEqualTo(1);
            assertThat(countOrganizations()).isEqualTo(1);
            verify(keycloakProvisioningClient, times(1))
                    .createUser(EMAIL, FULL_NAME, PASSWORD);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rejectsExpiredInvitationBeforeCreatingIamState() {
        insertInvitation(TOKEN, "PENDING", Instant.now().minusSeconds(3_600));

        ResponseEntity<ErrorResponse> response = accept(ErrorResponse.class);

        assertError(response, HttpStatus.GONE, "INVITATION_EXPIRED");
        assertThat(countUsers()).isZero();
        assertThat(countOrganizations()).isZero();
        verifyNoInteractions(keycloakProvisioningClient);
    }

    @Test
    void validatesPasswordBeforeCreatingIamState() {
        insertInvitation(TOKEN, "PENDING", Instant.now().plusSeconds(86_400));
        Map<String, String> invalidRequest = Map.of(
                "token", TOKEN,
                "fullName", FULL_NAME,
                "password", "short");

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                ENDPOINT,
                invalidRequest,
                ErrorResponse.class);

        assertError(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        assertThat(invitationStatus()).isEqualTo("PENDING");
        assertThat(countUsers()).isZero();
        verifyNoInteractions(keycloakProvisioningClient);
    }

    private void assertProvisionedBusinessState() {
        Map<String, Object> organization = jdbcTemplate.queryForMap(
                """
                        SELECT id, status
                        FROM iam.organizations
                        WHERE lower(name) = lower(?)
                        """,
                ORGANIZATION_NAME);
        assertThat(organization.get("status")).isEqualTo("ONBOARDING_INCOMPLETE");

        Map<String, Object> membership = jdbcTemplate.queryForMap(
                """
                        SELECT member.status, member.joined_at
                        FROM iam.organization_members member
                        JOIN iam.users user_account ON user_account.id = member.user_id
                        WHERE lower(user_account.email) = lower(?)
                        """,
                EMAIL);
        assertThat(membership.get("status")).isEqualTo("ACTIVE");
        assertThat(membership.get("joined_at")).isNotNull();

        String roleCode = jdbcTemplate.queryForObject(
                """
                        SELECT role_record.code
                        FROM iam.member_roles member_role
                        JOIN iam.organization_members member
                          ON member.id = member_role.member_id
                        JOIN iam.users user_account ON user_account.id = member.user_id
                        JOIN iam.roles role_record ON role_record.id = member_role.role_id
                        WHERE lower(user_account.email) = lower(?)
                        """,
                String.class,
                EMAIL);
        assertThat(roleCode).isEqualTo("AIRLINE_ADMIN");

        Map<String, Object> invitation = jdbcTemplate.queryForMap(
                """
                        SELECT status, organization_id, accepted_at
                        FROM iam.invitations
                        WHERE token_hash = ?
                        """,
                invitationTokenService.hash(TOKEN));
        assertThat(invitation.get("status")).isEqualTo("ACCEPTED");
        assertThat(invitation.get("organization_id"))
                .isEqualTo(organization.get("id"));
        assertThat(invitation.get("accepted_at")).isNotNull();
    }

    private Map<String, String> requestWithIgnoredIdentityFields() {
        return Map.of(
                "token", TOKEN,
                "fullName", "  " + FULL_NAME + "  ",
                "password", PASSWORD,
                "email", "attacker@demo.com",
                "organizationName", "Attacker Organization");
    }

    private <T> ResponseEntity<T> accept(Class<T> responseType) {
        return restTemplate.postForEntity(
                ENDPOINT,
                Map.of(
                        "token", TOKEN,
                        "fullName", FULL_NAME,
                        "password", PASSWORD),
                responseType);
    }

    private void insertInvitation(String token, String status, Instant expiresAt) {
        Instant now = Instant.now();
        int inserted = jdbcTemplate.update(
                """
                        INSERT INTO iam.invitations (
                            company_name, admin_email, token_hash, status,
                            created_by_user_id, expires_at, created_at, updated_at
                        )
                        SELECT ?, ?, ?, ?, user_account.id, ?, ?, ?
                        FROM iam.users user_account
                        WHERE lower(user_account.email) = 'platform.admin@demo.com'
                        """,
                ORGANIZATION_NAME,
                EMAIL,
                invitationTokenService.hash(token),
                status,
                Timestamp.from(expiresAt),
                Timestamp.from(now),
                Timestamp.from(now));
        assertThat(inserted).isEqualTo(1);
    }

    private void insertExistingUser(UserStatus status) {
        jdbcTemplate.update(
                """
                        INSERT INTO iam.users (
                            email, password_hash, full_name, status, auth_provider
                        ) VALUES (?, NULL, 'Existing K8 User', ?, 'KEYCLOAK')
                        """,
                EMAIL,
                status.name());
    }

    private Map<String, Object> userState() {
        return jdbcTemplate.queryForMap(
                """
                        SELECT status, keycloak_user_id
                        FROM iam.users
                        WHERE lower(email) = lower(?)
                        """,
                EMAIL);
    }

    private String invitationStatus() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM iam.invitations WHERE token_hash = ?",
                String.class,
                invitationTokenService.hash(TOKEN));
    }

    private int countUsers() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM iam.users WHERE lower(email) = lower(?)",
                Integer.class,
                EMAIL);
    }

    private int countOrganizations() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM iam.organizations WHERE lower(name) = lower(?)",
                Integer.class,
                ORGANIZATION_NAME);
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
                        WHERE user_id IN (
                            SELECT id FROM iam.users WHERE lower(email) = lower(?)
                        ) OR organization_id IN (
                            SELECT id FROM iam.organizations WHERE lower(name) = lower(?)
                        )
                        """,
                EMAIL,
                ORGANIZATION_NAME);
        jdbcTemplate.update(
                "DELETE FROM iam.invitations WHERE lower(admin_email) = lower(?)",
                EMAIL);
        jdbcTemplate.update(
                "DELETE FROM iam.users WHERE lower(email) = lower(?)",
                EMAIL);
        jdbcTemplate.update(
                "DELETE FROM iam.organizations WHERE lower(name) = lower(?)",
                ORGANIZATION_NAME);
    }

    private void assertError(
            ResponseEntity<ErrorResponse> response,
            HttpStatus status,
            String errorCode) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(errorCode);
        assertThat(response.getBody().path()).isEqualTo(ENDPOINT);
    }
}
