package com.aliozcan.airportops.iam_service.app.report;

import com.aliozcan.airportops.iam_service.testsupport.MockReportServiceConfig;
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
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@Import({TestJwtDecoderConfig.class, MockReportServiceConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w14proxy.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w14proxy.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w14proxy.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W14 Proxy %'",
        "INSERT INTO iam.users (email, full_name, status, auth_provider) VALUES "
                + "('tenant@w14proxy.test', 'W14 Proxy Tenant User', 'ACTIVE', 'KEYCLOAK')",
        "INSERT INTO iam.organizations (name, status) VALUES ('W14 Proxy Org', 'ACTIVE')",
        "INSERT INTO iam.organization_members (organization_id, user_id, status) "
                + "SELECT o.id, u.id, 'ACTIVE' FROM iam.users u "
                + "JOIN iam.organizations o ON u.email = 'tenant@w14proxy.test' "
                + "AND o.name = 'W14 Proxy Org'",
        "INSERT INTO iam.member_roles (member_id, role_id) "
                + "SELECT m.id, r.id FROM iam.organization_members m "
                + "JOIN iam.users u ON u.id = m.user_id "
                + "JOIN iam.roles r ON r.code = 'AIRLINE_ADMIN' "
                + "WHERE u.email = 'tenant@w14proxy.test'"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w14proxy.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w14proxy.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w14proxy.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W14 Proxy %'"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AppReportProxyIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockRestServiceServer mockReportServiceServer;

    @Test
    void forwardsDailyFlightsRequestWithIamJwtAndDateQueryParam() {
        UUID organizationId = organizationId();
        String reportServiceResponseBody =
                "{\"date\":\"2026-08-01\",\"totalFlights\":0,\"delayedFlights\":0,\"cancelledFlights\":0}";

        mockReportServiceServer.expect(requestTo(
                        "http://mock-report-service/organizations/" + organizationId
                                + "/reports/daily-flights?date=2026-08-01"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, startsWith("Bearer ")))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportServiceResponseBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.W14_PROXY_TENANT_TOKEN);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/reports/daily-flights?date=2026-08-01", HttpMethod.GET, new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(reportServiceResponseBody);
        mockReportServiceServer.verify();
    }

    @Test
    void forwardsGateUtilizationRequestWithIamJwtAndDateQueryParam() {
        UUID organizationId = organizationId();
        String reportServiceResponseBody = "[]";

        mockReportServiceServer.expect(requestTo(
                        "http://mock-report-service/organizations/" + organizationId
                                + "/reports/gate-utilization?date=2026-08-01"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, startsWith("Bearer ")))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportServiceResponseBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.W14_PROXY_TENANT_TOKEN);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/reports/gate-utilization?date=2026-08-01", HttpMethod.GET, new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(reportServiceResponseBody);
        mockReportServiceServer.verify();
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/app/reports/daily-flights?date=2026-08-01", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private UUID organizationId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM iam.organizations WHERE name = 'W14 Proxy Org'", UUID.class);
    }
}
