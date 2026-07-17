package com.aliozcan.airportops.iam_service.app.setup;

import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupProfileResponse;
import com.aliozcan.airportops.iam_service.auth.dto.ErrorResponse;
import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
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
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w5d.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w5d.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w5d.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W5D %'",
        "INSERT INTO iam.users (email, full_name, status, auth_provider) VALUES "
                + "('admin@w5d.test', 'W5D Admin', 'ACTIVE', 'KEYCLOAK'), "
                + "('member@w5d.test', 'W5D Member', 'ACTIVE', 'KEYCLOAK'), "
                + "('active-admin@w5d.test', 'W5D Active Admin', 'ACTIVE', 'KEYCLOAK')",
        "INSERT INTO iam.organizations (name, status) VALUES "
                + "('W5D Onboarding', 'ONBOARDING_INCOMPLETE'), "
                + "('W5D Member Org', 'ONBOARDING_INCOMPLETE'), "
                + "('W5D Active', 'ACTIVE')",
        "INSERT INTO iam.organization_members (organization_id, user_id, status) "
                + "SELECT o.id, u.id, 'ACTIVE' FROM iam.users u JOIN iam.organizations o ON "
                + "(u.email = 'admin@w5d.test' AND o.name = 'W5D Onboarding') OR "
                + "(u.email = 'member@w5d.test' AND o.name = 'W5D Member Org') OR "
                + "(u.email = 'active-admin@w5d.test' AND o.name = 'W5D Active')",
        "INSERT INTO iam.member_roles (member_id, role_id) "
                + "SELECT m.id, r.id FROM iam.organization_members m "
                + "JOIN iam.users u ON u.id = m.user_id "
                + "JOIN iam.roles r ON r.code = CASE "
                + "WHEN u.email = 'member@w5d.test' THEN 'VIEWER' ELSE 'AIRLINE_ADMIN' END "
                + "WHERE u.email LIKE '%@w5d.test'"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w5d.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w5d.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w5d.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W5D %'"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AppSetupProfileIntegrationTests {

    private static final String ENDPOINT = "/app/setup/profile";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void firstPutCreatesCanonicalNormalizedProfile() {
        Map<String, Object> request = validRequest();

        ResponseEntity<AppSetupProfileResponse> response = put(
                TestJwtDecoderConfig.W5D_ADMIN_TOKEN,
                request,
                AppSetupProfileResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().displayName()).isEqualTo("Example Airlines");
        assertThat(response.getBody().iataCode()).isEqualTo("B6");
        assertThat(response.getBody().icaoCode()).isEqualTo("EXA");
        assertThat(response.getBody().countryCode()).isEqualTo("TR");
        assertThat(response.getBody().baseAirportIata()).isEqualTo("IST");
        assertThat(response.getBody().operationsContactEmail())
                .isEqualTo("ops@example.com");
        assertThat(response.getBody().createdAt()).isNotNull();
        assertThat(response.getBody().updatedAt()).isNotNull();
        assertThat(profileCount("W5D Onboarding")).isEqualTo(1);
    }

    @Test
    void repeatedPutUpdatesSameRowAndPreservesCreatedAt() throws Exception {
        ResponseEntity<AppSetupProfileResponse> first = put(
                TestJwtDecoderConfig.W5D_ADMIN_TOKEN,
                validRequest(),
                AppSetupProfileResponse.class);
        Thread.sleep(5);
        Map<String, Object> update = validRequest();
        update.put("displayName", "Updated Airlines");
        update.put("iataCode", "UA");

        ResponseEntity<AppSetupProfileResponse> second = put(
                TestJwtDecoderConfig.W5D_ADMIN_TOKEN,
                update,
                AppSetupProfileResponse.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).isNotNull();
        assertThat(first.getBody()).isNotNull();
        assertThat(second.getBody().createdAt()).isEqualTo(first.getBody().createdAt());
        assertThat(second.getBody().updatedAt()).isAfter(first.getBody().updatedAt());
        assertThat(second.getBody().displayName()).isEqualTo("Updated Airlines");
        assertThat(profileCount("W5D Onboarding")).isEqualTo(1);
    }

    @Test
    void concurrentPutsDoNotCreateDuplicates() throws Exception {
        Map<String, Object> firstRequest = validRequest();
        firstRequest.put("displayName", "Concurrent One");
        Map<String, Object> secondRequest = validRequest();
        secondRequest.put("displayName", "Concurrent Two");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<ResponseEntity<AppSetupProfileResponse>> first =
                    CompletableFuture.supplyAsync(() -> put(
                            TestJwtDecoderConfig.W5D_ADMIN_TOKEN,
                            firstRequest,
                            AppSetupProfileResponse.class), executor);
            CompletableFuture<ResponseEntity<AppSetupProfileResponse>> second =
                    CompletableFuture.supplyAsync(() -> put(
                            TestJwtDecoderConfig.W5D_ADMIN_TOKEN,
                            secondRequest,
                            AppSetupProfileResponse.class), executor);

            assertThat(first.get().getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(second.get().getStatusCode()).isEqualTo(HttpStatus.OK);
        } finally {
            executor.shutdownNow();
        }
        assertThat(profileCount("W5D Onboarding")).isEqualTo(1);
        String displayName = jdbcTemplate.queryForObject("""
                SELECT p.display_name FROM iam.organization_setup_profiles p
                JOIN iam.organizations o ON o.id = p.organization_id
                WHERE o.name = 'W5D Onboarding'
                """, String.class);
        assertThat(displayName).isIn("Concurrent One", "Concurrent Two");
    }

    @Test
    void optionalBlankValuesBecomeNull() {
        Map<String, Object> request = validRequest();
        request.put("iataCode", "   ");
        request.put("icaoCode", " ");
        request.put("countryCode", "");
        request.put("timezone", "  ");
        request.put("baseAirportIata", " ");
        request.put("operationsContactEmail", " ");

        ResponseEntity<AppSetupProfileResponse> response = put(
                TestJwtDecoderConfig.W5D_ADMIN_TOKEN,
                request,
                AppSetupProfileResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().iataCode()).isNull();
        assertThat(response.getBody().icaoCode()).isNull();
        assertThat(response.getBody().countryCode()).isNull();
        assertThat(response.getBody().timezone()).isNull();
        assertThat(response.getBody().baseAirportIata()).isNull();
        assertThat(response.getBody().operationsContactEmail()).isNull();
    }

    @Test
    void invalidTimezoneReturnsValidationError() {
        assertValidationError(Map.of("timezone", "Not/A_Zone"));
    }

    @Test
    void invalidCountryReturnsValidationError() {
        assertValidationError(Map.of("countryCode", "ZZ"));
    }

    @Test
    void icaoCodeRejectsDigits() {
        assertValidationError(Map.of("icaoCode", "E1A"));
    }

    @Test
    void baseAirportIataRejectsDigits() {
        assertValidationError(Map.of("baseAirportIata", "I5T"));
    }

    @Test
    void requestOrganizationIdCannotSelectAnotherOrganization() {
        UUID memberOrganizationId = jdbcTemplate.queryForObject(
                "SELECT id FROM iam.organizations WHERE name = 'W5D Member Org'",
                UUID.class);
        Map<String, Object> request = validRequest();
        request.put("organizationId", memberOrganizationId.toString());

        ResponseEntity<AppSetupProfileResponse> response = put(
                TestJwtDecoderConfig.W5D_ADMIN_TOKEN,
                request,
                AppSetupProfileResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().organizationId()).isNotEqualTo(memberOrganizationId);
        assertThat(profileCount("W5D Onboarding")).isEqualTo(1);
        assertThat(profileCount("W5D Member Org")).isZero();
    }

    @Test
    void platformUserIsForbidden() {
        assertForbidden(TestJwtDecoderConfig.VALID_TOKEN);
    }

    @Test
    void nonAdminTenantMemberIsForbidden() {
        assertForbidden(TestJwtDecoderConfig.W5D_MEMBER_TOKEN);
    }

    @Test
    void activeOrganizationIsForbidden() {
        assertForbidden(TestJwtDecoderConfig.W5D_ACTIVE_ADMIN_TOKEN);
    }

    @Test
    void profileWriteDoesNotChangeOrganizationNameOrStatus() {
        put(TestJwtDecoderConfig.W5D_ADMIN_TOKEN, validRequest(), AppSetupProfileResponse.class);

        Map<String, Object> organization = jdbcTemplate.queryForMap("""
                SELECT name, status FROM iam.organizations
                WHERE name = 'W5D Onboarding'
                """);
        assertThat(organization.get("name")).isEqualTo("W5D Onboarding");
        assertThat(organization.get("status")).isEqualTo("ONBOARDING_INCOMPLETE");
    }

    private void assertValidationError(Map<String, Object> overrides) {
        Map<String, Object> request = validRequest();
        request.putAll(overrides);
        ResponseEntity<ErrorResponse> response = put(
                TestJwtDecoderConfig.W5D_ADMIN_TOKEN,
                request,
                ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
    }

    private void assertForbidden(String token) {
        ResponseEntity<ErrorResponse> response = put(
                token,
                validRequest(),
                ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    private Map<String, Object> validRequest() {
        return new java.util.HashMap<>(Map.of(
                "displayName", "  Example Airlines  ",
                "iataCode", " b6 ",
                "icaoCode", " exa ",
                "countryCode", " tr ",
                "timezone", " Europe/Istanbul ",
                "baseAirportIata", " ist ",
                "operationsContactEmail", " OPS@Example.COM "));
    }

    private int profileCount(String organizationName) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM iam.organization_setup_profiles p
                JOIN iam.organizations o ON o.id = p.organization_id
                WHERE o.name = ?
                """, Integer.class, organizationName);
    }

    private <T> ResponseEntity<T> put(
            String token,
            Object body,
            Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                ENDPOINT,
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                responseType);
    }
}
