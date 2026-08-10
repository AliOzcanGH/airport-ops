package com.aliozcan.airportops.audit_service.api;

import com.aliozcan.airportops.audit_service.testsupport.TestIamJwtDecoderConfig;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No @Sql cleanup here: audit.audit_logs is append-only (a DB trigger rejects
 * UPDATE/DELETE, see V2__audit_logs_immutability_trigger.sql), so every test
 * seeds rows with a fresh random resourceId and scopes assertions to that id
 * rather than relying on row counts that would accumulate across test runs.
 */
@Import(TestIamJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditLogControllerIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void tenantUserSeesOnlyTheirOwnOrganizationAuditLogs() {
        UUID orgAResourceId = UUID.randomUUID();
        UUID orgBResourceId = UUID.randomUUID();
        seedAuditLog(TestIamJwtDecoderConfig.ORG_A, "FLIGHT_CREATED", "FLIGHT", orgAResourceId);
        seedAuditLog(TestIamJwtDecoderConfig.ORG_B, "FLIGHT_CREATED", "FLIGHT", orgBResourceId);

        ResponseEntity<List> response = get(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/audit-logs",
                TestIamJwtDecoderConfig.ORG_A_ADMIN_TOKEN, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> resourceIds = resourceIdsOf(response.getBody());
        assertThat(resourceIds).contains(orgAResourceId.toString());
        assertThat(resourceIds).doesNotContain(orgBResourceId.toString());
    }

    @Test
    void tenantUserCannotReadAnotherOrganizationsAuditLogsViaPathOrgId() {
        seedAuditLog(TestIamJwtDecoderConfig.ORG_B, "FLIGHT_CREATED", "FLIGHT", UUID.randomUUID());

        ResponseEntity<Map> response = get(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_B + "/audit-logs",
                TestIamJwtDecoderConfig.ORG_A_ADMIN_TOKEN, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("errorCode")).isEqualTo("TENANT_MISMATCH");
    }

    @Test
    void tenantUserWithoutAuditReadPermissionIsRejected() {
        ResponseEntity<Map> response = get(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/audit-logs",
                TestIamJwtDecoderConfig.ORG_A_VIEWER_TOKEN, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("errorCode")).isEqualTo("MISSING_PERMISSION");
    }

    @Test
    void tenantUserCannotCallThePlatformOnlyEndpoint() {
        ResponseEntity<Map> response = get(
                "/platform/audit-logs", TestIamJwtDecoderConfig.ORG_A_ADMIN_TOKEN, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("errorCode")).isEqualTo("PLATFORM_ONLY");
    }

    @Test
    void platformAdminSeesAuditLogsAcrossAllOrganizations() {
        UUID orgAResourceId = UUID.randomUUID();
        UUID orgBResourceId = UUID.randomUUID();
        seedAuditLog(TestIamJwtDecoderConfig.ORG_A, "FLIGHT_CREATED", "FLIGHT", orgAResourceId);
        seedAuditLog(TestIamJwtDecoderConfig.ORG_B, "TASK_COMPLETED", "TASK", orgBResourceId);

        ResponseEntity<List> response = get(
                "/platform/audit-logs", TestIamJwtDecoderConfig.PLATFORM_ADMIN_TOKEN, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> resourceIds = resourceIdsOf(response.getBody());
        assertThat(resourceIds).contains(orgAResourceId.toString(), orgBResourceId.toString());
    }

    @Test
    void missingTokenIsRejected() {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Map> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/audit-logs",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @SuppressWarnings("unchecked")
    private List<String> resourceIdsOf(List<?> rows) {
        return rows.stream()
                .map(row -> (String) ((Map<String, Object>) row).get("resourceId"))
                .toList();
    }

    private <T> ResponseEntity<T> get(String path, String token, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }

    private void seedAuditLog(UUID organizationId, String action, String resourceType, UUID resourceId) {
        jdbcTemplate.update(
                "INSERT INTO audit.audit_logs "
                        + "(id, organization_id, action, resource_type, resource_id, occurred_at, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), organizationId, action, resourceType, resourceId,
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()));
    }
}
