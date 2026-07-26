package com.aliozcan.airportops.iam_service.app.dashboard;

import com.aliozcan.airportops.iam_service.app.dashboard.dto.AppDashboardOverviewResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w6.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w6.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w6.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W6 %'",
        "INSERT INTO iam.users (email, full_name, status, auth_provider, preferred_language) VALUES "
                + "('onboarding@w6.test', 'Onboarding Tenant', 'ACTIVE', 'KEYCLOAK', 'TR'), "
                + "('active@w6.test', 'Active Tenant', 'ACTIVE', 'KEYCLOAK', 'EN'), "
                + "('inactive-org@w6.test', 'Inactive Org Tenant', 'ACTIVE', 'KEYCLOAK', 'TR'), "
                + "('inactive-member@w6.test', 'Inactive Member Tenant', 'ACTIVE', 'KEYCLOAK', 'EN')",
        "INSERT INTO iam.organizations (name, status) VALUES "
                + "('W6 Onboarding', 'ONBOARDING_INCOMPLETE'), "
                + "('W6 Active', 'ACTIVE'), "
                + "('W6 Inactive', 'INACTIVE'), "
                + "('W6 Inactive Member', 'ACTIVE')",
        "INSERT INTO iam.organization_members (organization_id, user_id, status) "
                + "SELECT o.id, u.id, CASE "
                + "WHEN u.email = 'inactive-member@w6.test' THEN 'INACTIVE' "
                + "ELSE 'ACTIVE' END "
                + "FROM iam.users u JOIN iam.organizations o ON "
                + "(u.email = 'onboarding@w6.test' AND o.name = 'W6 Onboarding') OR "
                + "(u.email = 'active@w6.test' AND o.name = 'W6 Active') OR "
                + "(u.email = 'inactive-org@w6.test' AND o.name = 'W6 Inactive') OR "
                + "(u.email = 'inactive-member@w6.test' AND o.name = 'W6 Inactive Member')"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w6.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w6.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w6.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W6 %'"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AppDashboardOverviewIntegrationTests {

    private static final String ENDPOINT = "/app/dashboard/overview";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsUnauthorizedWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(ENDPOINT, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsPlatformOnlyUser() {
        ResponseEntity<ErrorResponse> response = get(
                TestJwtDecoderConfig.VALID_TOKEN, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    @Test
    void returnsOverviewForActiveTenant() {
        ResponseEntity<AppDashboardOverviewResponse> response = get(
                TestJwtDecoderConfig.W6_ACTIVE_TOKEN, AppDashboardOverviewResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().organizationName()).isEqualTo("W6 Active");
        assertThat(response.getBody().organizationStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void rejectsOnboardingIncompleteOrganization() {
        ResponseEntity<ErrorResponse> response = get(
                TestJwtDecoderConfig.W6_ONBOARDING_TOKEN, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    @Test
    void rejectsInactiveOrganization() {
        ResponseEntity<ErrorResponse> response = get(
                TestJwtDecoderConfig.W6_INACTIVE_ORG_TOKEN, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    @Test
    void rejectsInactiveMembership() {
        ResponseEntity<ErrorResponse> response = get(
                TestJwtDecoderConfig.W6_INACTIVE_MEMBER_TOKEN, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PERMISSION");
    }

    private <T> ResponseEntity<T> get(String token, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                ENDPOINT, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }
}
