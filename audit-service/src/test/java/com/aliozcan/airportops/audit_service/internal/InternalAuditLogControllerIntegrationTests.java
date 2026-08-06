package com.aliozcan.airportops.audit_service.internal;

import com.aliozcan.airportops.audit_service.config.InternalServiceSecretFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No @Sql cleanup here: audit.audit_logs is append-only (a DB trigger rejects
 * UPDATE/DELETE, see V2__audit_logs_immutability_trigger.sql), so tests use a
 * fresh random resourceId per run and scope every assertion to it instead of
 * relying on row counts that could accumulate across test runs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InternalAuditLogControllerIntegrationTests {

    private static final String CORRECT_SECRET = "local-dev-internal-secret";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void writesAuditLogWhenInternalSecretIsCorrect() {
        UUID organizationId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Map<String, Object> body = Map.of(
                "organizationId", organizationId,
                "actorUserId", UUID.randomUUID(),
                "actorEmail", "admin@example.com",
                "action", "MEMBER_ROLE_UPDATED",
                "resourceType", "MEMBER",
                "resourceId", memberId,
                "occurredAt", Instant.now().toString(),
                "metadata", Map.of("previousRole", "VIEWER", "newRole", "OPS_USER"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(InternalServiceSecretFilter.HEADER_NAME, CORRECT_SECRET);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/internal/audit-logs", new HttpEntity<>(body, headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit.audit_logs WHERE resource_id = ?", Integer.class, memberId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void rejectsRequestWithoutInternalSecretHeader() {
        Map<String, Object> body = Map.of(
                "action", "MEMBER_ROLE_UPDATED",
                "resourceType", "MEMBER",
                "occurredAt", Instant.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/internal/audit-logs", new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsRequestWithWrongInternalSecretHeader() {
        Map<String, Object> body = Map.of(
                "action", "MEMBER_ROLE_UPDATED",
                "resourceType", "MEMBER",
                "occurredAt", Instant.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(InternalServiceSecretFilter.HEADER_NAME, "wrong-secret");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/internal/audit-logs", new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
