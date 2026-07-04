package com.aliozcan.airportops.iam_service.platform.invitation;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InvitationValidationIntegrationTests {

    private static final String ENDPOINT = "/invitations/validate";
    private static final String VALID_TOKEN = "A".repeat(43);
    private static final String EXPIRED_PENDING_TOKEN = "B".repeat(43);
    private static final String ACCEPTED_TOKEN = "C".repeat(43);
    private static final String CANCELLED_TOKEN = "D".repeat(43);
    private static final String EXPIRED_STATUS_TOKEN = "E".repeat(43);
    private static final String UNKNOWN_TOKEN = "Z".repeat(43);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InvitationTokenService invitationTokenService;

    @BeforeEach
    void setUp() {
        cleanFixtures();
        insertInvitation(
                VALID_TOKEN,
                "PENDING",
                Instant.now().plusSeconds(86_400));
        insertInvitation(
                EXPIRED_PENDING_TOKEN,
                "PENDING",
                Instant.now().minusSeconds(3_600));
        insertInvitation(
                ACCEPTED_TOKEN,
                "ACCEPTED",
                Instant.now().plusSeconds(86_400));
        insertInvitation(
                CANCELLED_TOKEN,
                "CANCELLED",
                Instant.now().plusSeconds(86_400));
        insertInvitation(
                EXPIRED_STATUS_TOKEN,
                "EXPIRED",
                Instant.now().plusSeconds(86_400));
    }

    @AfterEach
    void tearDown() {
        cleanFixtures();
    }

    @Test
    void validatesPendingInvitationWithoutBearerTokenOrDatabaseMutation()
            throws Exception {
        Map<String, Object> before = invitationSnapshot(VALID_TOKEN);

        ResponseEntity<String> first = postToken(VALID_TOKEN, String.class);
        ResponseEntity<String> second = postToken(VALID_TOKEN, String.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode json = objectMapper.readTree(first.getBody());
        assertThat(json.get("organizationName").asText())
                .isEqualTo("Pegasus Airlines");
        assertThat(json.get("invitedEmail").asText())
                .isEqualTo("ad***@k7a.integration.test");
        assertThat(json.hasNonNull("expiresAt")).isTrue();
        List<String> responseFields = new ArrayList<>();
        json.fieldNames().forEachRemaining(responseFields::add);
        assertThat(responseFields).containsExactlyInAnyOrder(
                "organizationName",
                "invitedEmail",
                "expiresAt");
        assertThat(invitationSnapshot(VALID_TOKEN)).isEqualTo(before);
    }

    @Test
    void returnsNotFoundForUnknownWellFormedToken() {
        assertError(
                postToken(UNKNOWN_TOKEN, ErrorResponse.class),
                HttpStatus.NOT_FOUND,
                "INVITATION_NOT_FOUND");
    }

    @Test
    void returnsNotFoundForCancelledInvitation() {
        assertError(
                postToken(CANCELLED_TOKEN, ErrorResponse.class),
                HttpStatus.NOT_FOUND,
                "INVITATION_NOT_FOUND");
    }

    @Test
    void rejectsAcceptedInvitation() {
        assertError(
                postToken(ACCEPTED_TOKEN, ErrorResponse.class),
                HttpStatus.CONFLICT,
                "INVITATION_ALREADY_USED");
    }

    @Test
    void rejectsExpiredPendingInvitationWithoutUpdatingIt() {
        Map<String, Object> before = invitationSnapshot(EXPIRED_PENDING_TOKEN);

        assertError(
                postToken(EXPIRED_PENDING_TOKEN, ErrorResponse.class),
                HttpStatus.GONE,
                "INVITATION_EXPIRED");

        assertThat(invitationSnapshot(EXPIRED_PENDING_TOKEN)).isEqualTo(before);
    }

    @Test
    void rejectsInvitationWithExpiredStatusWithoutUpdatingIt() {
        Map<String, Object> before = invitationSnapshot(EXPIRED_STATUS_TOKEN);

        assertError(
                postToken(EXPIRED_STATUS_TOKEN, ErrorResponse.class),
                HttpStatus.GONE,
                "INVITATION_EXPIRED");

        assertThat(invitationSnapshot(EXPIRED_STATUS_TOKEN)).isEqualTo(before);
    }

    @Test
    void rejectsMissingOrNullToken() {
        assertValidationError("{}");
        assertValidationError("{\"token\":null}");
    }

    @Test
    void rejectsBlankWhitespaceOrMalformedToken() {
        assertValidationError("{\"token\":\"\"}");
        assertValidationError("{\"token\":\"   \"}");
        assertValidationError("{\"token\":\"not-valid!\"}");
    }

    private void insertInvitation(
            String rawToken,
            String status,
            Instant expiresAt) {
        Instant now = Instant.now();
        String localPart = rawToken.equals(VALID_TOKEN)
                ? "admin"
                : rawToken.substring(0, 1).toLowerCase(Locale.ROOT);
        int inserted = jdbcTemplate.update(
                """
                        INSERT INTO iam.invitations (
                            id,
                            company_name,
                            admin_email,
                            token_hash,
                            status,
                            created_by_user_id,
                            expires_at,
                            created_at,
                            updated_at
                        )
                        SELECT ?, ?, ?, ?, ?, user_account.id, ?, ?, ?
                        FROM iam.users user_account
                        WHERE lower(user_account.email) = 'platform.admin@demo.com'
                        """,
                UUID.randomUUID(),
                "Pegasus Airlines",
                localPart + "@k7a.integration.test",
                invitationTokenService.hash(rawToken),
                status,
                Timestamp.from(expiresAt),
                Timestamp.from(now),
                Timestamp.from(now));
        assertThat(inserted).isEqualTo(1);
    }

    private Map<String, Object> invitationSnapshot(String rawToken) {
        return jdbcTemplate.queryForMap(
                """
                        SELECT status, expires_at, updated_at
                        FROM iam.invitations
                        WHERE token_hash = ?
                        """,
                invitationTokenService.hash(rawToken));
    }

    private void cleanFixtures() {
        jdbcTemplate.update(
                "DELETE FROM iam.invitations WHERE admin_email LIKE ?",
                "%@k7a.integration.test");
    }

    private <T> ResponseEntity<T> postToken(
            String token,
            Class<T> responseType) {
        return restTemplate.postForEntity(
                ENDPOINT,
                Map.of("token", token),
                responseType);
    }

    private void assertValidationError(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                ENDPOINT,
                new HttpEntity<>(body, headers),
                ErrorResponse.class);
        assertError(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
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
}
