package com.aliozcan.airportops.iam_service.app.flight;

import com.aliozcan.airportops.iam_service.testsupport.MockFlightServiceConfig;
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

@Import({TestJwtDecoderConfig.class, MockFlightServiceConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w10.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w10.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w10.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W10 %'",
        "INSERT INTO iam.users (email, full_name, status, auth_provider) VALUES "
                + "('tenant@w10.test', 'W10 Tenant User', 'ACTIVE', 'KEYCLOAK')",
        "INSERT INTO iam.organizations (name, status) VALUES ('W10 Tenant Org', 'ACTIVE')",
        "INSERT INTO iam.organization_members (organization_id, user_id, status) "
                + "SELECT o.id, u.id, 'ACTIVE' FROM iam.users u "
                + "JOIN iam.organizations o ON u.email = 'tenant@w10.test' "
                + "AND o.name = 'W10 Tenant Org'",
        "INSERT INTO iam.member_roles (member_id, role_id) "
                + "SELECT m.id, r.id FROM iam.organization_members m "
                + "JOIN iam.users u ON u.id = m.user_id "
                + "JOIN iam.roles r ON r.code = 'AIRLINE_ADMIN' "
                + "WHERE u.email = 'tenant@w10.test'"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM iam.member_roles WHERE member_id IN "
                + "(SELECT id FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w10.test'))",
        "DELETE FROM iam.organization_members WHERE user_id IN "
                + "(SELECT id FROM iam.users WHERE email LIKE '%@w10.test')",
        "DELETE FROM iam.users WHERE email LIKE '%@w10.test'",
        "DELETE FROM iam.organizations WHERE name LIKE 'W10 %'"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AppFlightProxyIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockRestServiceServer mockFlightServiceServer;

    @Test
    void forwardsCreateFlightWithIamJwtAndReturnsFlightServiceResponse() {
        UUID organizationId = organizationId();
        UUID gateId = UUID.randomUUID();

        String flightServiceResponseBody =
                "{\"id\":\"" + UUID.randomUUID() + "\",\"organizationId\":\"" + organizationId
                        + "\",\"flightNumber\":\"PC123\",\"status\":\"SCHEDULED\",\"assignedGateId\":\""
                        + gateId + "\"}";

        mockFlightServiceServer.expect(requestTo(
                        "http://mock-flight-service/organizations/" + organizationId + "/flights"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, org.hamcrest.Matchers.startsWith("Bearer ")))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(flightServiceResponseBody));

        ResponseEntity<String> response = createFlight(TestJwtDecoderConfig.W10_TENANT_TOKEN,
                "{\"flightNumber\":\"PC123\",\"origin\":\"SAW\",\"destination\":\"IST\","
                        + "\"scheduledDeparture\":\"2026-09-01T10:00:00Z\","
                        + "\"scheduledArrival\":\"2026-09-01T11:00:00Z\",\"assignedGateId\":\""
                        + gateId + "\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(flightServiceResponseBody);
        mockFlightServiceServer.verify();
    }

    @Test
    void forwardsListFlights() {
        UUID organizationId = organizationId();
        String flightServiceResponseBody = "[]";

        mockFlightServiceServer.expect(requestTo(
                        "http://mock-flight-service/organizations/" + organizationId + "/flights"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, org.hamcrest.Matchers.startsWith("Bearer ")))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(flightServiceResponseBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.W10_TENANT_TOKEN);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/flights",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(flightServiceResponseBody);
        mockFlightServiceServer.verify();
    }

    @Test
    void relaysFlightServiceErrorResponseAsIs() {
        UUID organizationId = organizationId();
        UUID gateId = UUID.randomUUID();

        String errorBody = "{\"errorCode\":\"GATE_NOT_FOUND\"}";
        mockFlightServiceServer.expect(requestTo(
                        "http://mock-flight-service/organizations/" + organizationId + "/flights"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));

        ResponseEntity<String> response = createFlight(TestJwtDecoderConfig.W10_TENANT_TOKEN,
                "{\"flightNumber\":\"PC123\",\"origin\":\"SAW\",\"destination\":\"IST\","
                        + "\"scheduledDeparture\":\"2026-09-01T10:00:00Z\","
                        + "\"scheduledArrival\":\"2026-09-01T11:00:00Z\",\"assignedGateId\":\""
                        + gateId + "\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("GATE_NOT_FOUND");
    }

    @Test
    void forwardsUpdateFlightStatusWithIamJwtAndReturnsFlightServiceResponse() {
        UUID organizationId = organizationId();
        UUID flightId = UUID.randomUUID();

        String flightServiceResponseBody =
                "{\"id\":\"" + flightId + "\",\"organizationId\":\"" + organizationId
                        + "\",\"flightNumber\":\"PC123\",\"status\":\"BOARDING\"}";

        mockFlightServiceServer.expect(requestTo(
                        "http://mock-flight-service/organizations/" + organizationId
                                + "/flights/" + flightId + "/status"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, org.hamcrest.Matchers.startsWith("Bearer ")))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(flightServiceResponseBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.W10_TENANT_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/flights/" + flightId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>("{\"status\":\"BOARDING\"}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(flightServiceResponseBody);
        mockFlightServiceServer.verify();
    }

    @Test
    void relaysFlightServiceStatusUpdateErrorResponseAsIs() {
        UUID organizationId = organizationId();
        UUID flightId = UUID.randomUUID();

        String errorBody = "{\"errorCode\":\"INVALID_STATUS_TRANSITION\"}";
        mockFlightServiceServer.expect(requestTo(
                        "http://mock-flight-service/organizations/" + organizationId
                                + "/flights/" + flightId + "/status"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.W10_TENANT_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/flights/" + flightId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>("{\"status\":\"DEPARTED\"}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("INVALID_STATUS_TRANSITION");
    }

    @Test
    void rejectsStatusUpdateWithoutBearerToken() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/flights/" + UUID.randomUUID() + "/status",
                HttpMethod.PUT,
                new HttpEntity<>("{\"status\":\"BOARDING\"}"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void forwardsListTasksWithIamJwtAndReturnsFlightServiceResponse() {
        UUID organizationId = organizationId();
        UUID flightId = UUID.randomUUID();
        String flightServiceResponseBody = "[]";

        mockFlightServiceServer.expect(requestTo(
                        "http://mock-flight-service/organizations/" + organizationId
                                + "/flights/" + flightId + "/tasks"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, org.hamcrest.Matchers.startsWith("Bearer ")))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(flightServiceResponseBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.W10_TENANT_TOKEN);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/flights/" + flightId + "/tasks",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(flightServiceResponseBody);
        mockFlightServiceServer.verify();
    }

    @Test
    void forwardsUpdateTaskStatusWithIamJwtAndReturnsFlightServiceResponse() {
        UUID organizationId = organizationId();
        UUID flightId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        String flightServiceResponseBody =
                "{\"id\":\"" + taskId + "\",\"flightId\":\"" + flightId + "\",\"status\":\"IN_PROGRESS\"}";

        mockFlightServiceServer.expect(requestTo(
                        "http://mock-flight-service/organizations/" + organizationId
                                + "/flights/" + flightId + "/tasks/" + taskId + "/status"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, org.hamcrest.Matchers.startsWith("Bearer ")))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(flightServiceResponseBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.W10_TENANT_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/flights/" + flightId + "/tasks/" + taskId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>("{\"status\":\"IN_PROGRESS\"}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(flightServiceResponseBody);
        mockFlightServiceServer.verify();
    }

    @Test
    void relaysTaskStatusUpdateErrorResponseAsIs() {
        UUID organizationId = organizationId();
        UUID flightId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        String errorBody = "{\"errorCode\":\"TASK_NOT_FOUND\"}";
        mockFlightServiceServer.expect(requestTo(
                        "http://mock-flight-service/organizations/" + organizationId
                                + "/flights/" + flightId + "/tasks/" + taskId + "/status"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwtDecoderConfig.W10_TENANT_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/flights/" + flightId + "/tasks/" + taskId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>("{\"status\":\"IN_PROGRESS\"}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("TASK_NOT_FOUND");
    }

    @Test
    void rejectsListTasksWithoutBearerToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/app/flights/" + UUID.randomUUID() + "/tasks", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "/app/flights",
                HttpMethod.POST,
                new HttpEntity<>("{}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private UUID organizationId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM iam.organizations WHERE name = 'W10 Tenant Org'", UUID.class);
    }

    private ResponseEntity<String> createFlight(String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                "/app/flights",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
    }
}
