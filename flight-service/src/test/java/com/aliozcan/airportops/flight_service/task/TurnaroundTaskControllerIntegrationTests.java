package com.aliozcan.airportops.flight_service.task;

import com.aliozcan.airportops.flight_service.flight.dto.CreateFlightRequest;
import com.aliozcan.airportops.flight_service.flight.dto.FlightResponse;
import com.aliozcan.airportops.flight_service.testsupport.MockAirportServiceConfig;
import com.aliozcan.airportops.flight_service.testsupport.TestIamJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@Import({TestIamJwtDecoderConfig.class, MockAirportServiceConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "DELETE FROM flight.turnaround_tasks WHERE flight_id IN "
                + "(SELECT id FROM flight.flights WHERE organization_id IN "
                + "('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222'))",
        "DELETE FROM flight.flights WHERE organization_id IN "
                + "('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222')"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM flight.turnaround_tasks WHERE flight_id IN "
                + "(SELECT id FROM flight.flights WHERE organization_id IN "
                + "('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222'))",
        "DELETE FROM flight.flights WHERE organization_id IN "
                + "('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222')"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class TurnaroundTaskControllerIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockRestServiceServer mockAirportServiceServer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listsSixAutoCreatedTasksForAuthorizedReader() {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PC301");

        ResponseEntity<Map[]> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/flights/" + flightId + "/tasks",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(TestIamJwtDecoderConfig.VIEWER_TOKEN)),
                Map[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(6);
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN,IN_PROGRESS",
            "IN_PROGRESS,DONE",
            "IN_PROGRESS,BLOCKED",
            "BLOCKED,IN_PROGRESS"
    })
    void acceptsValidTaskStatusTransition(String fromStatus, String toStatus) {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PT" + fromStatus.charAt(0) + toStatus.charAt(0));
        UUID taskId = firstTaskId(flightId);
        setTaskStatus(taskId, fromStatus);

        ResponseEntity<Map> response = updateTaskStatus(
                TestIamJwtDecoderConfig.ORG_A, flightId, taskId, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                toStatus, "Ali Ozcan", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(toStatus);
        assertThat(response.getBody().get("assignedTo")).isEqualTo("Ali Ozcan");
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN,DONE",
            "OPEN,BLOCKED",
            "DONE,IN_PROGRESS",
            "DONE,OPEN",
            "BLOCKED,DONE"
    })
    void rejectsInvalidTaskStatusTransition(String fromStatus, String toStatus) {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PX" + fromStatus.charAt(0) + toStatus.charAt(0));
        UUID taskId = firstTaskId(flightId);
        setTaskStatus(taskId, fromStatus);

        ResponseEntity<String> response = updateTaskStatus(
                TestIamJwtDecoderConfig.ORG_A, flightId, taskId, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                toStatus, null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("INVALID_STATUS_TRANSITION");
    }

    @Test
    void rejectsTaskUpdateWithoutTaskCompletePermission() {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PC302");
        UUID taskId = firstTaskId(flightId);

        ResponseEntity<String> response = updateTaskStatus(
                TestIamJwtDecoderConfig.ORG_A, flightId, taskId, TestIamJwtDecoderConfig.VIEWER_TOKEN,
                "IN_PROGRESS", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("MISSING_PERMISSION");
    }

    @Test
    void rejectsTaskUpdatePathOrganizationMismatch() {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PC303");
        UUID taskId = firstTaskId(flightId);

        ResponseEntity<String> response = updateTaskStatus(
                TestIamJwtDecoderConfig.ORG_A, flightId, taskId, TestIamJwtDecoderConfig.OTHER_ORG_ADMIN_TOKEN,
                "IN_PROGRESS", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("TENANT_MISMATCH");
    }

    @Test
    void rejectsTaskUpdateForFlightBelongingToAnotherOrganization() {
        UUID otherOrgFlightId = seedFlight(TestIamJwtDecoderConfig.ORG_B, "PC304");
        UUID taskId = firstTaskId(otherOrgFlightId);

        ResponseEntity<String> response = updateTaskStatus(
                TestIamJwtDecoderConfig.ORG_A, otherOrgFlightId, taskId, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                "IN_PROGRESS", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("FLIGHT_NOT_FOUND");
    }

    @Test
    void rejectsTaskUpdateWhenTaskBelongsToAnotherFlightInSameOrganization() {
        // Fifth-level ownership check: org matches, and flightId-A is real and owned
        // by ORG_A, but taskId actually belongs to flightId-B (also ORG_A). Must not
        // be allowed just because the org and the first flight both check out.
        // MockRestServiceServer requires every expectation to be registered before
        // any request is sent, so both gate lookups are set up first.
        UUID gateA = UUID.randomUUID();
        UUID gateB = UUID.randomUUID();
        expectGateLookup(TestIamJwtDecoderConfig.ORG_A, gateA, TestIamJwtDecoderConfig.ADMIN_TOKEN);
        expectGateLookup(TestIamJwtDecoderConfig.ORG_A, gateB, TestIamJwtDecoderConfig.ADMIN_TOKEN);

        UUID flightA = createFlight(TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN, gateA, "PC305");
        UUID flightB = createFlight(TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN, gateB, "PC306");
        UUID taskOnFlightB = firstTaskId(flightB);

        ResponseEntity<String> response = updateTaskStatus(
                TestIamJwtDecoderConfig.ORG_A, flightA, taskOnFlightB, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                "IN_PROGRESS", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("TASK_NOT_FOUND");

        String statusInDb = jdbcTemplate.queryForObject(
                "SELECT status FROM flight.turnaround_tasks WHERE id = ?", String.class, taskOnFlightB);
        assertThat(statusInDb).isEqualTo("OPEN");
    }

    @Test
    void rejectsTaskUpdateWithoutBearerToken() {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PC307");
        UUID taskId = firstTaskId(flightId);

        ResponseEntity<String> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/flights/" + flightId
                        + "/tasks/" + taskId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("status", "IN_PROGRESS")),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private UUID seedFlight(UUID organizationId, String flightNumber) {
        String creationToken = organizationId.equals(TestIamJwtDecoderConfig.ORG_A)
                ? TestIamJwtDecoderConfig.ADMIN_TOKEN
                : TestIamJwtDecoderConfig.OTHER_ORG_ADMIN_TOKEN;
        UUID gateId = UUID.randomUUID();
        expectGateLookup(organizationId, gateId, creationToken);
        return createFlight(organizationId, creationToken, gateId, flightNumber);
    }

    private void expectGateLookup(UUID organizationId, UUID gateId, String token) {
        mockAirportServiceServer.expect(requestTo(
                        "http://mock-airport-service/organizations/" + organizationId + "/gates/" + gateId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":\"" + gateId + "\",\"stationId\":\"" + UUID.randomUUID()
                                + "\",\"code\":\"A1\",\"status\":\"ACTIVE\"}"));
    }

    private UUID createFlight(UUID organizationId, String token, UUID gateId, String flightNumber) {
        CreateFlightRequest request = new CreateFlightRequest(
                flightNumber, "SAW", "IST",
                Instant.parse("2026-09-01T10:00:00Z"), Instant.parse("2026-09-01T11:00:00Z"), gateId);

        ResponseEntity<FlightResponse> created = restTemplate.exchange(
                "/organizations/" + organizationId + "/flights",
                HttpMethod.POST,
                new HttpEntity<>(request, bearerHeaders(token)),
                FlightResponse.class);
        return created.getBody().id();
    }

    private UUID firstTaskId(UUID flightId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM flight.turnaround_tasks WHERE flight_id = ? ORDER BY task_type LIMIT 1",
                UUID.class, flightId);
    }

    private void setTaskStatus(UUID taskId, String status) {
        if ("OPEN".equals(status)) return;
        jdbcTemplate.update(
                "UPDATE flight.turnaround_tasks SET status = ? WHERE id = ?", status, taskId);
    }

    private <T> ResponseEntity<T> updateTaskStatus(
            UUID orgId, UUID flightId, UUID taskId, String token, String status, String assignedTo,
            Class<T> responseType) {
        Map<String, Object> body = assignedTo == null
                ? Map.of("status", status)
                : Map.of("status", status, "assignedTo", assignedTo);
        return restTemplate.exchange(
                "/organizations/" + orgId + "/flights/" + flightId + "/tasks/" + taskId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(body, bearerHeaders(token)),
                responseType);
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
