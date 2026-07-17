package com.aliozcan.airportops.iam_service.app.setup;

import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupOverviewResponse;
import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
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
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w5a.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w5a.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w5a.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W5A %'",
        "INSERT INTO iam.users (email, full_name, status, auth_provider, preferred_language) VALUES "
                + "('onboarding@w5a.test', 'Onboarding Tenant', 'ACTIVE', 'KEYCLOAK', 'TR'), "
                + "('active@w5a.test', 'Active Tenant', 'ACTIVE', 'KEYCLOAK', 'EN'), "
                + "('inactive-org@w5a.test', 'Inactive Org Tenant', 'ACTIVE', 'KEYCLOAK', 'TR'), "
                + "('inactive-member@w5a.test', 'Inactive Member Tenant', 'ACTIVE', 'KEYCLOAK', 'EN')",
        "INSERT INTO iam.organizations (name, status) VALUES "
                + "('W5A Onboarding', 'ONBOARDING_INCOMPLETE'), "
                + "('W5A Active', 'ACTIVE'), "
                + "('W5A Inactive', 'INACTIVE'), "
                + "('W5A Inactive Member', 'ONBOARDING_INCOMPLETE')",
        "INSERT INTO iam.organization_members (organization_id, user_id, status) "
                + "SELECT o.id, u.id, CASE "
                + "WHEN u.email = 'inactive-member@w5a.test' THEN 'INACTIVE' "
                + "ELSE 'ACTIVE' END "
                + "FROM iam.users u JOIN iam.organizations o ON "
                + "(u.email = 'onboarding@w5a.test' AND o.name = 'W5A Onboarding') OR "
                + "(u.email = 'active@w5a.test' AND o.name = 'W5A Active') OR "
                + "(u.email = 'inactive-org@w5a.test' AND o.name = 'W5A Inactive') OR "
                + "(u.email = 'inactive-member@w5a.test' AND o.name = 'W5A Inactive Member')"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w5a.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w5a.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w5a.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W5A %'"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AppSetupOverviewIntegrationTests {

    private static final String ENDPOINT = "/app/setup/overview";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsUnauthorizedWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                ENDPOINT,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsPlatformOnlyUser() {
        ResponseEntity<ErrorResponse> response = get(
                TestJwtDecoderConfig.VALID_TOKEN,
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    @Test
    void returnsOverviewForOnboardingTenant() {
        ResponseEntity<AppSetupOverviewResponse> response = get(
                TestJwtDecoderConfig.W5A_ONBOARDING_TOKEN,
                AppSetupOverviewResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().organizationName()).isEqualTo("W5A Onboarding");
        assertThat(response.getBody().organizationStatus().name())
                .isEqualTo("ONBOARDING_INCOMPLETE");
        assertThat(response.getBody().preferredLanguage().name()).isEqualTo("TR");
        assertThat(response.getBody().steps())
                .extracting("key")
                .containsExactly("PROFILE", "STATION", "REVIEW");
        assertThat(response.getBody().steps())
                .extracting("status")
                .containsExactly("NOT_STARTED", "LOCKED", "LOCKED");
        assertThat(response.getBody().profile()).isNull();
    }

    @Test
    void includesCanonicalSavedProfileWithoutChangingExistingFields() {
        jdbcTemplate.update("""
                INSERT INTO iam.organization_setup_profiles (
                    organization_id, display_name, iata_code, icao_code,
                    country_code, timezone, base_airport_iata,
                    operations_contact_email)
                SELECT id, 'Example Airlines', 'B6', 'EXA', 'TR',
                       'Europe/Istanbul', 'IST', 'ops@example.com'
                FROM iam.organizations WHERE name = 'W5A Onboarding'
                """);

        ResponseEntity<AppSetupOverviewResponse> response = get(
                TestJwtDecoderConfig.W5A_ONBOARDING_TOKEN,
                AppSetupOverviewResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().organizationName()).isEqualTo("W5A Onboarding");
        assertThat(response.getBody().organizationStatus().name())
                .isEqualTo("ONBOARDING_INCOMPLETE");
        assertThat(response.getBody().steps()).hasSize(3);
        assertThat(response.getBody().profile()).isNotNull();
        assertThat(response.getBody().profile().displayName())
                .isEqualTo("Example Airlines");
        assertThat(response.getBody().profile().iataCode()).isEqualTo("B6");
    }

    @Test
    void returnsOverviewForActiveTenant() {
        ResponseEntity<AppSetupOverviewResponse> response = get(
                TestJwtDecoderConfig.W5A_ACTIVE_TOKEN,
                AppSetupOverviewResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().organizationName()).isEqualTo("W5A Active");
        assertThat(response.getBody().organizationStatus().name()).isEqualTo("ACTIVE");
        assertThat(response.getBody().preferredLanguage().name()).isEqualTo("EN");
    }

    @Test
    void rejectsInactiveOrganization() {
        ResponseEntity<ErrorResponse> response = get(
                TestJwtDecoderConfig.W5A_INACTIVE_ORG_TOKEN,
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    @Test
    void rejectsInactiveMembership() {
        ResponseEntity<ErrorResponse> response = get(
                TestJwtDecoderConfig.W5A_INACTIVE_MEMBER_TOKEN,
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    private <T> ResponseEntity<T> get(String token, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                responseType);
    }
}
