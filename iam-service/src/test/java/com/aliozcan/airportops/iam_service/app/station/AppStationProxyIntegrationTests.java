package com.aliozcan.airportops.iam_service.app.station;

import com.aliozcan.airportops.iam_service.testsupport.MockAirportServiceConfig;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@Import({TestJwtDecoderConfig.class, MockAirportServiceConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w8b.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w8b.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w8b.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W8B %'",
        "INSERT INTO iam.users (email, full_name, status, auth_provider) VALUES "
                + "('tenant@w8b.test', 'W8B Tenant User', 'ACTIVE', 'KEYCLOAK')",
        "INSERT INTO iam.organizations (name, status) VALUES ('W8B Tenant Org', 'ACTIVE')",
        "INSERT INTO iam.organization_members (organization_id, user_id, status) "
                + "SELECT o.id, u.id, 'ACTIVE' FROM iam.users u "
                + "JOIN iam.organizations o ON u.email = 'tenant@w8b.test' "
                + "AND o.name = 'W8B Tenant Org'",
        "INSERT INTO iam.member_roles (member_id, role_id) "
                + "SELECT m.id, r.id FROM iam.organization_members m "
                + "JOIN iam.users u ON u.id = m.user_id "
                + "JOIN iam.roles r ON r.code = 'AIRLINE_ADMIN' "
                + "WHERE u.email = 'tenant@w8b.test'"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w8b.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w8b.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w8b.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W8B %'"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AppStationProxyIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockRestServiceServer mockAirportServiceServer;

    @Test
    void forwardsCreateStationWithIamJwtAndReturnsAirportServiceResponse() {
        UUID organizationId = jdbcTemplate.queryForObject(
                "SELECT id FROM iam.organizations WHERE name = 'W8B Tenant Org'", UUID.class);

        String airportServiceResponseBody =
                "{\"id\":\"" + UUID.randomUUID() + "\",\"organizationId\":\"" + organizationId
                        + "\",\"stationName\":\"SAW Station\",\"airportCode\":\"SAW\",\"gateCount\":8}";

        mockAirportServiceServer.expect(requestTo(
                        "http://mock-airport-service/organizations/" + organizationId + "/stations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, org.hamcrest.Matchers.startsWith("Bearer ")))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(airportServiceResponseBody));

        ResponseEntity<String> response = createStation(TestJwtDecoderConfig.W8B_TENANT_TOKEN,
                "{\"stationName\":\"SAW Station\",\"airportCode\":\"SAW\",\"gateCount\":8}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(airportServiceResponseBody);
        mockAirportServiceServer.verify();
    }

    @Test
    void forwardsListStationsWithIamJwtAndReturnsAirportServiceResponse() {
        UUID organizationId = jdbcTemplate.queryForObject(
                "SELECT id FROM iam.organizations WHERE name = 'W8B Tenant Org'", UUID.class);
        String airportServiceResponseBody = "[]";

        mockAirportServiceServer.expect(requestTo(
                        "http://mock-airport-service/organizations/" + organizationId + "/stations"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, org.hamcrest.Matchers.startsWith("Bearer ")))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(airportServiceResponseBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.W8B_TENANT_TOKEN);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/stations", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(airportServiceResponseBody);
        mockAirportServiceServer.verify();
    }

    @Test
    void rejectsListStationsWithoutBearerToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/app/stations", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void relaysAirportServiceErrorResponseAsIs() {
        UUID organizationId = jdbcTemplate.queryForObject(
                "SELECT id FROM iam.organizations WHERE name = 'W8B Tenant Org'", UUID.class);

        String errorBody = "{\"errorCode\":\"TENANT_MISMATCH\"}";
        mockAirportServiceServer.expect(requestTo(
                        "http://mock-airport-service/organizations/" + organizationId + "/stations"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));

        ResponseEntity<String> response = createStation(TestJwtDecoderConfig.W8B_TENANT_TOKEN,
                "{\"stationName\":\"SAW Station\",\"airportCode\":\"SAW\",\"gateCount\":8}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("TENANT_MISMATCH");
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/stations",
                HttpMethod.POST,
                new HttpEntity<>("{}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<String> createStation(String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                "/app/stations",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
    }
}
