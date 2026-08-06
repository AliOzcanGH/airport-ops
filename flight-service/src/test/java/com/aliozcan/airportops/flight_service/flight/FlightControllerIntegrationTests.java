package com.aliozcan.airportops.flight_service.flight;

import com.aliozcan.airportops.flight_service.flight.dto.CreateFlightRequest;
import com.aliozcan.airportops.flight_service.flight.dto.FlightResponse;
import com.aliozcan.airportops.flight_service.testsupport.MockAirportServiceConfig;
import com.aliozcan.airportops.flight_service.testsupport.TestIamJwtDecoderConfig;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
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
class FlightControllerIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockRestServiceServer mockAirportServiceServer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsFlightForAuthorizedAdminWithActiveGate() {
        UUID gateId = UUID.randomUUID();
        expectGateLookup(TestIamJwtDecoderConfig.ORG_A, gateId, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                gateSnapshotBody(gateId, "ACTIVE"));

        ResponseEntity<FlightResponse> response = createFlight(
                TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                flightRequestBody("PC101", gateId, Instant.parse("2026-09-01T10:00:00Z"),
                        Instant.parse("2026-09-01T11:00:00Z")),
                FlightResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().organizationId()).isEqualTo(TestIamJwtDecoderConfig.ORG_A);
        assertThat(response.getBody().status()).isEqualTo("SCHEDULED");
        assertThat(response.getBody().assignedGateId()).isEqualTo(gateId);
        mockAirportServiceServer.verify();

        List<String> taskTypes = jdbcTemplate.queryForList(
                "SELECT task_type FROM flight.turnaround_tasks WHERE flight_id = ? ORDER BY task_type",
                String.class, response.getBody().id());
        assertThat(taskTypes).containsExactlyInAnyOrder(
                "CLEANING", "CATERING", "FUELING", "BAGGAGE_LOADING",
                "BOARDING_PREPARATION", "SECURITY_CHECK");
        List<String> taskStatuses = jdbcTemplate.queryForList(
                "SELECT DISTINCT status FROM flight.turnaround_tasks WHERE flight_id = ?",
                String.class, response.getBody().id());
        assertThat(taskStatuses).containsExactly("OPEN");
    }

    @Test
    void rejectsUserWithoutFlightCreatePermission() {
        UUID gateId = UUID.randomUUID();

        ResponseEntity<String> response = createFlight(
                TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.VIEWER_TOKEN,
                flightRequestBody("PC101", gateId, Instant.parse("2026-09-01T10:00:00Z"),
                        Instant.parse("2026-09-01T11:00:00Z")),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("MISSING_PERMISSION");
    }

    @Test
    void rejectsPathOrganizationMismatch() {
        UUID gateId = UUID.randomUUID();

        ResponseEntity<String> response = createFlight(
                TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.OTHER_ORG_ADMIN_TOKEN,
                flightRequestBody("PC101", gateId, Instant.parse("2026-09-01T10:00:00Z"),
                        Instant.parse("2026-09-01T11:00:00Z")),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("TENANT_MISMATCH");
    }

    @Test
    void rejectsCrossTenantGateAssignment() {
        // The gate id belongs to another organization; airport-service's own
        // tenant/ownership check on the relayed token is what produces this 404 —
        // flight-service must not assume the gate is valid just because an id was supplied.
        UUID gateId = UUID.randomUUID();
        mockAirportServiceServer.expect(requestTo(
                        "http://mock-airport-service/organizations/" + TestIamJwtDecoderConfig.ORG_A
                                + "/gates/" + gateId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TestIamJwtDecoderConfig.ADMIN_TOKEN))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errorCode\":\"GATE_NOT_FOUND\"}"));

        ResponseEntity<String> response = createFlight(
                TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                flightRequestBody("PC101", gateId, Instant.parse("2026-09-01T10:00:00Z"),
                        Instant.parse("2026-09-01T11:00:00Z")),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("GATE_NOT_FOUND");
        mockAirportServiceServer.verify();
    }

    @Test
    void rejectsGateThatIsNotActive() {
        UUID gateId = UUID.randomUUID();
        expectGateLookup(TestIamJwtDecoderConfig.ORG_A, gateId, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                gateSnapshotBody(gateId, "MAINTENANCE"));

        ResponseEntity<String> response = createFlight(
                TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                flightRequestBody("PC101", gateId, Instant.parse("2026-09-01T10:00:00Z"),
                        Instant.parse("2026-09-01T11:00:00Z")),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("GATE_NOT_ACTIVE");
    }

    @Test
    void rejectsOverlappingFlightOnSameGate() {
        UUID gateId = UUID.randomUUID();
        expectGateLookup(TestIamJwtDecoderConfig.ORG_A, gateId, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                gateSnapshotBody(gateId, "ACTIVE"));
        expectGateLookup(TestIamJwtDecoderConfig.ORG_A, gateId, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                gateSnapshotBody(gateId, "ACTIVE"));

        ResponseEntity<FlightResponse> first = createFlight(
                TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                flightRequestBody("PC101", gateId, Instant.parse("2026-09-01T10:00:00Z"),
                        Instant.parse("2026-09-01T11:00:00Z")),
                FlightResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> second = createFlight(
                TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                flightRequestBody("PC202", gateId, Instant.parse("2026-09-01T10:30:00Z"),
                        Instant.parse("2026-09-01T11:30:00Z")),
                String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).contains("GATE_CONFLICT");
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        UUID gateId = UUID.randomUUID();
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/flights",
                flightRequestBody("PC101", gateId, Instant.parse("2026-09-01T10:00:00Z"),
                        Instant.parse("2026-09-01T11:00:00Z")),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void expectGateLookup(UUID orgId, UUID gateId, String token, String responseBody) {
        mockAirportServiceServer.expect(requestTo(
                        "http://mock-airport-service/organizations/" + orgId + "/gates/" + gateId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, startsWith("Bearer " + token)))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody));
    }

    private String gateSnapshotBody(UUID gateId, String status) {
        return "{\"id\":\"" + gateId + "\",\"stationId\":\"" + UUID.randomUUID()
                + "\",\"code\":\"A1\",\"status\":\"" + status + "\"}";
    }

    private CreateFlightRequest flightRequestBody(
            String flightNumber, UUID gateId, Instant departure, Instant arrival) {
        return new CreateFlightRequest(flightNumber, "SAW", "IST", departure, arrival, gateId);
    }

    private <T> ResponseEntity<T> createFlight(
            UUID orgId, String token, CreateFlightRequest request, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                "/organizations/" + orgId + "/flights",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                responseType);
    }
}
