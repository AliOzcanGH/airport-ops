package com.aliozcan.airportops.flight_service.flight;

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
        "DELETE FROM flight.flights WHERE organization_id IN "
                + "('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222')"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM flight.flights WHERE organization_id IN "
                + "('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222')"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class FlightStatusTransitionIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockRestServiceServer mockAirportServiceServer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @CsvSource({
            "SCHEDULED,BOARDING",
            "BOARDING,DEPARTED",
            "SCHEDULED,DELAYED",
            "DELAYED,BOARDING",
            "SCHEDULED,CANCELLED",
            "DELAYED,CANCELLED"
    })
    void acceptsValidStatusTransition(String fromStatus, String toStatus) {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PC" + fromStatus.charAt(0) + toStatus.charAt(0), fromStatus);

        ResponseEntity<FlightResponse> response = updateStatus(
                TestIamJwtDecoderConfig.ORG_A, flightId, TestIamJwtDecoderConfig.ADMIN_TOKEN, toStatus, FlightResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(toStatus);
        assertThat(response.getBody().id()).isEqualTo(flightId);
    }

    @ParameterizedTest
    @CsvSource({
            "DEPARTED,BOARDING",
            "DEPARTED,SCHEDULED",
            "CANCELLED,BOARDING",
            "CANCELLED,SCHEDULED",
            "SCHEDULED,DEPARTED"
    })
    void rejectsInvalidStatusTransition(String fromStatus, String toStatus) {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PI" + fromStatus.charAt(0) + toStatus.charAt(0), fromStatus);

        ResponseEntity<String> response = updateStatus(
                TestIamJwtDecoderConfig.ORG_A, flightId, TestIamJwtDecoderConfig.ADMIN_TOKEN, toStatus, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("INVALID_STATUS_TRANSITION");
        assertThat(response.getBody()).contains(fromStatus);
        assertThat(response.getBody()).contains(toStatus);
    }

    @Test
    void rejectsStatusUpdateWithoutFlightUpdatePermission() {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PC001", "SCHEDULED");

        ResponseEntity<String> response = updateStatus(
                TestIamJwtDecoderConfig.ORG_A, flightId, TestIamJwtDecoderConfig.VIEWER_TOKEN, "BOARDING", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("MISSING_PERMISSION");
    }

    @Test
    void rejectsStatusUpdatePathOrganizationMismatch() {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PC002", "SCHEDULED");

        ResponseEntity<String> response = updateStatus(
                TestIamJwtDecoderConfig.ORG_A, flightId, TestIamJwtDecoderConfig.OTHER_ORG_ADMIN_TOKEN,
                "BOARDING", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("TENANT_MISMATCH");
    }

    @Test
    void rejectsStatusUpdateForFlightBelongingToAnotherOrganization() {
        // The flight physically exists (owned by ORG_B) but the caller authenticates
        // as ORG_A. This must not leak existence or allow the update — same
        // ownership-verification pattern as W9/W10's station/gate checks.
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_B, "PC003", "SCHEDULED");

        ResponseEntity<String> response = updateStatus(
                TestIamJwtDecoderConfig.ORG_A, flightId, TestIamJwtDecoderConfig.ADMIN_TOKEN, "BOARDING", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("FLIGHT_NOT_FOUND");

        String statusInDb = jdbcTemplate.queryForObject(
                "SELECT status FROM flight.flights WHERE id = ?", String.class, flightId);
        assertThat(statusInDb).isEqualTo("SCHEDULED");
    }

    @Test
    void rejectsStatusUpdateWithoutBearerToken() {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PC004", "SCHEDULED");

        ResponseEntity<String> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/flights/" + flightId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("status", "BOARDING")),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsStatusUpdateWithUnknownStatusValue() {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PC005", "SCHEDULED");

        ResponseEntity<String> response = updateStatus(
                TestIamJwtDecoderConfig.ORG_A, flightId, TestIamJwtDecoderConfig.ADMIN_TOKEN, "LANDED", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("VALIDATION_ERROR");
    }

    @Test
    void getsSingleFlightForAuthorizedReader() {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_A, "PC006", "SCHEDULED");

        ResponseEntity<FlightResponse> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/flights/" + flightId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(TestIamJwtDecoderConfig.ADMIN_TOKEN)),
                FlightResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(flightId);
    }

    @Test
    void getSingleFlightRejectsCrossOrganizationLookup() {
        UUID flightId = seedFlight(TestIamJwtDecoderConfig.ORG_B, "PC007", "SCHEDULED");

        ResponseEntity<String> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/flights/" + flightId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(TestIamJwtDecoderConfig.ADMIN_TOKEN)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("FLIGHT_NOT_FOUND");
    }

    private UUID seedFlight(UUID organizationId, String flightNumber, String initialStatus) {
        String creationToken = organizationId.equals(TestIamJwtDecoderConfig.ORG_A)
                ? TestIamJwtDecoderConfig.ADMIN_TOKEN
                : TestIamJwtDecoderConfig.OTHER_ORG_ADMIN_TOKEN;

        UUID gateId = UUID.randomUUID();
        mockAirportServiceServer.expect(requestTo(
                        "http://mock-airport-service/organizations/" + organizationId + "/gates/" + gateId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + creationToken))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":\"" + gateId + "\",\"stationId\":\"" + UUID.randomUUID()
                                + "\",\"code\":\"A1\",\"status\":\"ACTIVE\"}"));

        CreateFlightRequest request = new CreateFlightRequest(
                flightNumber, "SAW", "IST",
                Instant.parse("2026-09-01T10:00:00Z"), Instant.parse("2026-09-01T11:00:00Z"), gateId);

        HttpHeaders headers = bearerHeaders(creationToken);
        ResponseEntity<FlightResponse> created = restTemplate.exchange(
                "/organizations/" + organizationId + "/flights",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                FlightResponse.class);
        UUID flightId = created.getBody().id();

        if (!"SCHEDULED".equals(initialStatus)) {
            jdbcTemplate.update("UPDATE flight.flights SET status = ? WHERE id = ?", initialStatus, flightId);
        }
        return flightId;
    }

    private <T> ResponseEntity<T> updateStatus(
            UUID orgId, UUID flightId, String token, String targetStatus, Class<T> responseType) {
        HttpHeaders headers = bearerHeaders(token);
        return restTemplate.exchange(
                "/organizations/" + orgId + "/flights/" + flightId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("status", targetStatus), headers),
                responseType);
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
