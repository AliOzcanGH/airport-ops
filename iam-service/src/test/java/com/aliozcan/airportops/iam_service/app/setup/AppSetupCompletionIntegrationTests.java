package com.aliozcan.airportops.iam_service.app.setup;

import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupCompletionResponse;
import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
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
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w5e.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w5e.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w5e.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W5E %'",
        "INSERT INTO iam.users (email, full_name, status, auth_provider) VALUES "
                + "('admin@w5e.test', 'W5E Admin', 'ACTIVE', 'KEYCLOAK'), "
                + "('member@w5e.test', 'W5E Member', 'ACTIVE', 'KEYCLOAK'), "
                + "('active-admin@w5e.test', 'W5E Active Admin', 'ACTIVE', 'KEYCLOAK')",
        "INSERT INTO iam.organizations (name, status, updated_at) VALUES "
                + "('W5E Onboarding', 'ONBOARDING_INCOMPLETE', '2026-07-01T00:00:00Z'), "
                + "('W5E Member Org', 'ONBOARDING_INCOMPLETE', '2026-07-01T00:00:00Z'), "
                + "('W5E Active', 'ACTIVE', '2026-07-01T00:00:00Z')",
        "INSERT INTO iam.organization_members (organization_id, user_id, status) "
                + "SELECT o.id, u.id, 'ACTIVE' FROM iam.users u JOIN iam.organizations o ON "
                + "(u.email = 'admin@w5e.test' AND o.name = 'W5E Onboarding') OR "
                + "(u.email = 'member@w5e.test' AND o.name = 'W5E Member Org') OR "
                + "(u.email = 'active-admin@w5e.test' AND o.name = 'W5E Active')",
        "INSERT INTO iam.member_roles (member_id, role_id) "
                + "SELECT m.id, r.id FROM iam.organization_members m "
                + "JOIN iam.users u ON u.id = m.user_id "
                + "JOIN iam.roles r ON r.code = CASE "
                + "WHEN u.email = 'member@w5e.test' THEN 'VIEWER' ELSE 'AIRLINE_ADMIN' END "
                + "WHERE u.email LIKE '%@w5e.test'"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w5e.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w5e.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w5e.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W5E %'"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AppSetupCompletionIntegrationTests {

    private static final String ENDPOINT = "/app/setup/complete";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void airlineAdminCompletesSetupWithoutRequestBody() {
        insertProfile("W5E Onboarding", "TR", "Europe/Istanbul", "ops@w5e.test");
        Instant previousUpdatedAt = organizationUpdatedAt("W5E Onboarding");

        ResponseEntity<AppSetupCompletionResponse> response = post(
                TestJwtDecoderConfig.W5E_ADMIN_TOKEN,
                null,
                AppSetupCompletionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().organizationId())
                .isEqualTo(organizationId("W5E Onboarding"));
        assertThat(response.getBody().organizationStatus())
                .isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(response.getBody().completedAt()).isAfter(previousUpdatedAt);
        assertThat(organizationStatus("W5E Onboarding")).isEqualTo("ACTIVE");
        assertThat(organizationUpdatedAt("W5E Onboarding"))
                .isCloseTo(response.getBody().completedAt(),
                        org.assertj.core.api.Assertions.within(1, ChronoUnit.MICROS));
    }

    @Test
    void inactiveDeletedAdminOrganizationDoesNotBlockOnboardingCompletion() {
        jdbcTemplate.update("""
                INSERT INTO iam.organizations (name, status)
                VALUES ('W5E Old Inactive', 'INACTIVE')
                """);
        jdbcTemplate.update("""
                INSERT INTO iam.organization_members (
                    organization_id, user_id, status, deleted_at)
                SELECT o.id, u.id, 'INACTIVE', now()
                FROM iam.organizations o
                JOIN iam.users u ON u.email = 'admin@w5e.test'
                WHERE o.name = 'W5E Old Inactive'
                """);
        jdbcTemplate.update("""
                INSERT INTO iam.member_roles (member_id, role_id)
                SELECT m.id, r.id
                FROM iam.organization_members m
                JOIN iam.organizations o ON o.id = m.organization_id
                JOIN iam.roles r ON r.code = 'AIRLINE_ADMIN'
                WHERE o.name = 'W5E Old Inactive'
                """);
        insertProfile("W5E Onboarding", "TR", "Europe/Istanbul", "ops@w5e.test");

        ResponseEntity<AppSetupCompletionResponse> response = post(
                TestJwtDecoderConfig.W5E_ADMIN_TOKEN,
                null,
                AppSetupCompletionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().organizationId())
                .isEqualTo(organizationId("W5E Onboarding"));
        assertThat(organizationStatus("W5E Old Inactive")).isEqualTo("INACTIVE");
    }

    @Test
    void clientSuppliedOrganizationIdDoesNotSelectAnotherOrganization() {
        insertProfile("W5E Onboarding", "TR", "Europe/Istanbul", "ops@w5e.test");
        UUID otherOrganizationId = organizationId("W5E Member Org");

        ResponseEntity<AppSetupCompletionResponse> response = post(
                TestJwtDecoderConfig.W5E_ADMIN_TOKEN,
                Map.of("organizationId", otherOrganizationId.toString()),
                AppSetupCompletionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().organizationId())
                .isEqualTo(organizationId("W5E Onboarding"));
        assertThat(response.getBody().organizationId()).isNotEqualTo(otherOrganizationId);
        assertThat(organizationStatus("W5E Member Org"))
                .isEqualTo("ONBOARDING_INCOMPLETE");
    }

    @Test
    void missingProfileReturnsProfileRequiredConflict() {
        assertConflict("SETUP_PROFILE_REQUIRED");
    }

    @Test
    void missingCountryCodeReturnsProfileIncompleteConflict() {
        insertProfile("W5E Onboarding", null, "Europe/Istanbul", "ops@w5e.test");
        assertConflict("SETUP_PROFILE_INCOMPLETE");
    }

    @Test
    void missingTimezoneReturnsProfileIncompleteConflict() {
        insertProfile("W5E Onboarding", "TR", null, "ops@w5e.test");
        assertConflict("SETUP_PROFILE_INCOMPLETE");
    }

    @Test
    void missingOperationsContactEmailReturnsProfileIncompleteConflict() {
        insertProfile("W5E Onboarding", "TR", "Europe/Istanbul", null);
        assertConflict("SETUP_PROFILE_INCOMPLETE");
    }

    @Test
    void platformUserIsForbidden() {
        assertForbidden(TestJwtDecoderConfig.VALID_TOKEN);
    }

    @Test
    void tenantMemberWithoutAirlineAdminRoleIsForbidden() {
        assertForbidden(TestJwtDecoderConfig.W5E_MEMBER_TOKEN);
    }

    @Test
    void alreadyActiveOrganizationReturnsConflictAndRemainsActive() {
        ResponseEntity<ErrorResponse> response = post(
                TestJwtDecoderConfig.W5E_ACTIVE_ADMIN_TOKEN,
                null,
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("SETUP_ALREADY_COMPLETED");
        assertThat(organizationStatus("W5E Active")).isEqualTo("ACTIVE");
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                ENDPOINT,
                HttpEntity.EMPTY,
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void assertConflict(String errorCode) {
        ResponseEntity<ErrorResponse> response = post(
                TestJwtDecoderConfig.W5E_ADMIN_TOKEN,
                null,
                ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(errorCode);
        assertThat(organizationStatus("W5E Onboarding"))
                .isEqualTo("ONBOARDING_INCOMPLETE");
    }

    private void assertForbidden(String token) {
        ResponseEntity<ErrorResponse> response = post(token, null, ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    private <T> ResponseEntity<T> post(String token, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return restTemplate.postForEntity(
                ENDPOINT,
                new HttpEntity<>(body, headers),
                responseType);
    }

    private void insertProfile(
            String organizationName,
            String countryCode,
            String timezone,
            String operationsContactEmail) {
        jdbcTemplate.update("""
                INSERT INTO iam.organization_setup_profiles (
                    organization_id,
                    display_name,
                    country_code,
                    timezone,
                    operations_contact_email
                ) SELECT id, 'W5E Airline Profile', ?, ?, ?
                  FROM iam.organizations
                 WHERE name = ?
                """, countryCode, timezone, operationsContactEmail, organizationName);
    }

    private UUID organizationId(String organizationName) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM iam.organizations WHERE name = ?",
                UUID.class,
                organizationName);
    }

    private String organizationStatus(String organizationName) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM iam.organizations WHERE name = ?",
                String.class,
                organizationName);
    }

    private Instant organizationUpdatedAt(String organizationName) {
        return jdbcTemplate.queryForObject(
                "SELECT updated_at FROM iam.organizations WHERE name = ?",
                Instant.class,
                organizationName);
    }
}
