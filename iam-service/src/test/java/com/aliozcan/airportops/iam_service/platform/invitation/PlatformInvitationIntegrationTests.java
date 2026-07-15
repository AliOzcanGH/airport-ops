package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationStatus;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.CreatePlatformInvitationRequest;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.PlatformInvitationResponse;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "DELETE FROM iam.invitations WHERE lower(admin_email) IN "
                + "('admin@pegasus.demo', 'second@pegasus.demo')",
        "DELETE FROM iam.platform_user_roles WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE lower(email) = "
                + "'k4.permissionless@integration.test')",
        "DELETE FROM iam.users WHERE lower(email) = "
                + "'k4.permissionless@integration.test'",
        "INSERT INTO iam.users (email, password_hash, full_name, status) VALUES "
                + "('k4.permissionless@integration.test', 'not-used', "
                + "'K6 Permissionless Test User', 'ACTIVE')"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.invitations WHERE lower(admin_email) IN "
                + "('admin@pegasus.demo', 'second@pegasus.demo')",
        "DELETE FROM iam.platform_user_roles WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE lower(email) = "
                + "'k4.permissionless@integration.test')",
        "DELETE FROM iam.users WHERE lower(email) = "
                + "'k4.permissionless@integration.test'"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PlatformInvitationIntegrationTests {

    private static final String ENDPOINT = "/platform/invitations";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InvitationTokenService invitationTokenService;

    @Test
    void createsPendingInvitationWithHashedToken() throws Exception {
        Instant requestStartedAt = Instant.now();
        ResponseEntity<String> response = createInvitation(
                TestJwtDecoderConfig.VALID_TOKEN,
                new CreatePlatformInvitationRequest(
                        "  Admin@Pegasus.Demo  ",
                        "  Pegasus Airlines  "),
                String.class);
        Instant requestCompletedAt = Instant.now();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("email").asText()).isEqualTo("admin@pegasus.demo");
        assertThat(json.get("organizationName").asText()).isEqualTo("Pegasus Airlines");
        assertThat(json.get("status").asText()).isEqualTo("PENDING");
        assertThat(json.get("emailDeliveryStatus").asText()).isEqualTo("FAILED");
        assertThat(json.get("emailSentAt").isNull()).isTrue();
        assertThat(json.get("devAcceptLink").asText())
                .startsWith("http://127.0.0.1:5173/invitations/accept?token=");
        assertThat(json.has("invitationToken")).isFalse();
        assertThat(json.has("tokenHash")).isFalse();
        assertThat(json.has("emailFailureReason")).isFalse();

        UUID invitationId = UUID.fromString(json.get("id").asText());
        Instant expiresAt = Instant.parse(json.get("expiresAt").asText());
        assertThat(expiresAt)
                .isBetween(
                        requestStartedAt.plus(Duration.ofHours(72)),
                        requestCompletedAt.plus(Duration.ofHours(72)).plusSeconds(1));

        Map<String, Object> persisted = jdbcTemplate.queryForMap(
                """
                        SELECT token_hash, created_by_user_id,
                               email_delivery_status, email_failure_reason
                        FROM iam.invitations
                        WHERE id = ?
                        """,
                invitationId);
        String rawToken = tokenFromAcceptLink(json.get("devAcceptLink").asText());
        assertThat(rawToken).matches("[A-Za-z0-9_-]{43}");
        String persistedHash = (String) persisted.get("token_hash");
        assertThat(persistedHash)
                .isNotEqualTo(rawToken)
                .isEqualTo(invitationTokenService.hash(rawToken));
        assertThat(persisted.get("email_delivery_status")).isEqualTo("FAILED");
        assertThat((String) persisted.get("email_failure_reason"))
                .contains("app.mail.from")
                .doesNotContain(rawToken)
                .doesNotContain(persistedHash);

        UUID platformAdminId = jdbcTemplate.queryForObject(
                "SELECT id FROM iam.users WHERE lower(email) = 'platform.admin@demo.com'",
                UUID.class);
        assertThat(persisted.get("created_by_user_id")).isEqualTo(platformAdminId);
    }

    @Test
    void rejectsDuplicatePendingInvitation() {
        CreatePlatformInvitationRequest request = validRequest();
        ResponseEntity<PlatformInvitationResponse> first = createInvitation(
                TestJwtDecoderConfig.VALID_TOKEN,
                request,
                PlatformInvitationResponse.class);

        ResponseEntity<ErrorResponse> second = createInvitation(
                TestJwtDecoderConfig.VALID_TOKEN,
                request,
                ErrorResponse.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertError(second, HttpStatus.CONFLICT, "PENDING_INVITATION_EXISTS");
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                ENDPOINT,
                validRequest(),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsInvalidBearerToken() {
        ResponseEntity<String> response = createInvitation(
                TestJwtDecoderConfig.INVALID_TOKEN,
                validRequest(),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsPermissionlessUser() {
        ResponseEntity<ErrorResponse> response = createInvitation(
                TestJwtDecoderConfig.PERMISSIONLESS_TOKEN,
                validRequest(),
                ErrorResponse.class);

        assertError(response, HttpStatus.FORBIDDEN, "MISSING_PERMISSION");
    }

    @Test
    void rejectsUnprovisionedUser() {
        ResponseEntity<ErrorResponse> response = createInvitation(
                TestJwtDecoderConfig.UNPROVISIONED_TOKEN,
                validRequest(),
                ErrorResponse.class);

        assertError(response, HttpStatus.FORBIDDEN, "USER_NOT_PROVISIONED");
    }

    @Test
    void rejectsInvalidEmail() {
        ResponseEntity<ErrorResponse> response = createInvitation(
                TestJwtDecoderConfig.VALID_TOKEN,
                new CreatePlatformInvitationRequest("not-an-email", "Pegasus Airlines"),
                ErrorResponse.class);

        assertError(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }

    @Test
    void rejectsBlankFields() {
        ResponseEntity<ErrorResponse> response = createInvitation(
                TestJwtDecoderConfig.VALID_TOKEN,
                new CreatePlatformInvitationRequest("   ", "   "),
                ErrorResponse.class);

        assertError(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }

    private CreatePlatformInvitationRequest validRequest() {
        return new CreatePlatformInvitationRequest(
                "admin@pegasus.demo",
                "Pegasus Airlines");
    }

    private <T> ResponseEntity<T> createInvitation(
            String token,
            CreatePlatformInvitationRequest request,
            Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                responseType);
    }

    private void assertError(
            ResponseEntity<ErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedErrorCode) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(expectedStatus.value());
        assertThat(response.getBody().error()).isEqualTo(expectedStatus.name());
        assertThat(response.getBody().errorCode()).isEqualTo(expectedErrorCode);
        assertThat(response.getBody().path()).isEqualTo(ENDPOINT);
    }

    private String tokenFromAcceptLink(String value) {
        String query = URI.create(value).getRawQuery();
        for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", 2);
            if (parts.length == 2 && parts[0].equals("token")) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("token query parameter not found");
    }
}
