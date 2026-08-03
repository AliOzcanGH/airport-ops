package com.aliozcan.airportops.iam_service.app.gate;

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
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w9.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w9.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w9.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W9 %'",
        "INSERT INTO iam.users (email, full_name, status, auth_provider) VALUES "
                + "('tenant@w9.test', 'W9 Tenant User', 'ACTIVE', 'KEYCLOAK')",
        "INSERT INTO iam.organizations (name, status) VALUES ('W9 Tenant Org', 'ACTIVE')",
        "INSERT INTO iam.organization_members (organization_id, user_id, status) "
                + "SELECT o.id, u.id, 'ACTIVE' FROM iam.users u "
                + "JOIN iam.organizations o ON u.email = 'tenant@w9.test' "
                + "AND o.name = 'W9 Tenant Org'",
        "INSERT INTO iam.member_roles (member_id, role_id) "
                + "SELECT m.id, r.id FROM iam.organization_members m "
                + "JOIN iam.users u ON u.id = m.user_id "
                + "JOIN iam.roles r ON r.code = 'AIRLINE_ADMIN' "
                + "WHERE u.email = 'tenant@w9.test'"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w9.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w9.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w9.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W9 %'"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AppGateProxyIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockRestServiceServer mockAirportServiceServer;

    @Test
    void forwardsCreateGateWithIamJwtAndReturnsAirportServiceResponse() {
        UUID organizationId = organizationId();
        UUID stationId = UUID.randomUUID();
        UUID gateId = UUID.randomUUID();

        String airportServiceResponseBody =
                "{\"id\":\"" + gateId + "\",\"stationId\":\"" + stationId
                        + "\",\"code\":\"A1\",\"terminal\":\"International\",\"status\":\"ACTIVE\"}";

        mockAirportServiceServer.expect(requestTo(
                        "http://mock-airport-service/organizations/" + organizationId
                                + "/stations/" + stationId + "/gates"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, org.hamcrest.Matchers.startsWith("Bearer ")))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(airportServiceResponseBody));

        ResponseEntity<String> response = createGate(stationId,
                "{\"code\":\"A1\",\"terminal\":\"International\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(airportServiceResponseBody);
        mockAirportServiceServer.verify();
    }

    @Test
    void forwardsListGates() {
        UUID organizationId = organizationId();
        UUID stationId = UUID.randomUUID();

        String airportServiceResponseBody = "[]";
        mockAirportServiceServer.expect(requestTo(
                        "http://mock-airport-service/organizations/" + organizationId
                                + "/stations/" + stationId + "/gates"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, org.hamcrest.Matchers.startsWith("Bearer ")))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(airportServiceResponseBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.W9_TENANT_TOKEN);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/stations/" + stationId + "/gates",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(airportServiceResponseBody);
        mockAirportServiceServer.verify();
    }

    @Test
    void forwardsUpdateGateStatus() {
        UUID organizationId = organizationId();
        UUID stationId = UUID.randomUUID();
        UUID gateId = UUID.randomUUID();

        String airportServiceResponseBody =
                "{\"id\":\"" + gateId + "\",\"stationId\":\"" + stationId
                        + "\",\"code\":\"A1\",\"status\":\"MAINTENANCE\"}";

        mockAirportServiceServer.expect(requestTo(
                        "http://mock-airport-service/organizations/" + organizationId
                                + "/stations/" + stationId + "/gates/" + gateId + "/status"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, org.hamcrest.Matchers.startsWith("Bearer ")))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(airportServiceResponseBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.W9_TENANT_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/stations/" + stationId + "/gates/" + gateId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>("{\"status\":\"MAINTENANCE\"}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(airportServiceResponseBody);
        mockAirportServiceServer.verify();
    }

    @Test
    void relaysAirportServiceErrorResponseAsIs() {
        UUID organizationId = organizationId();
        UUID stationId = UUID.randomUUID();

        String errorBody = "{\"errorCode\":\"STATION_NOT_FOUND\"}";
        mockAirportServiceServer.expect(requestTo(
                        "http://mock-airport-service/organizations/" + organizationId
                                + "/stations/" + stationId + "/gates"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));

        ResponseEntity<String> response = createGate(stationId,
                "{\"code\":\"A1\",\"terminal\":\"International\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("STATION_NOT_FOUND");
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/stations/" + UUID.randomUUID() + "/gates",
                HttpMethod.POST,
                new HttpEntity<>("{}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private UUID organizationId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM iam.organizations WHERE name = 'W9 Tenant Org'", UUID.class);
    }

    private ResponseEntity<String> createGate(UUID stationId, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.W9_TENANT_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                "/app/stations/" + stationId + "/gates",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
    }
}
