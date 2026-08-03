package com.aliozcan.airportops.airport_service.gate;

import com.aliozcan.airportops.airport_service.gate.dto.CreateGateRequest;
import com.aliozcan.airportops.airport_service.gate.dto.GateResponse;
import com.aliozcan.airportops.airport_service.gate.dto.UpdateGateStatusRequest;
import com.aliozcan.airportops.airport_service.testsupport.TestIamJwtDecoderConfig;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestIamJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {
        "DELETE FROM airport.gates WHERE station_id IN "
                + "(SELECT id FROM airport.stations WHERE station_name LIKE 'W9 Gate Test Station%')",
        "DELETE FROM airport.stations WHERE station_name LIKE 'W9 Gate Test Station%'",
        "INSERT INTO airport.stations (id, organization_id, station_name, airport_code, gate_count) "
                + "VALUES ('aaaaaaaa-0000-0000-0000-000000000001', "
                + "'11111111-1111-1111-1111-111111111111', 'W9 Gate Test Station A', 'GTA', 4)",
        "INSERT INTO airport.stations (id, organization_id, station_name, airport_code, gate_count) "
                + "VALUES ('bbbbbbbb-0000-0000-0000-000000000002', "
                + "'22222222-2222-2222-2222-222222222222', 'W9 Gate Test Station B', 'GTB', 4)"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM airport.gates WHERE station_id IN "
                + "(SELECT id FROM airport.stations WHERE station_name LIKE 'W9 Gate Test Station%')",
        "DELETE FROM airport.stations WHERE station_name LIKE 'W9 Gate Test Station%'"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class GateControllerIntegrationTests {

    private static final UUID STATION_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID STATION_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsGateForAuthorizedAdmin() {
        ResponseEntity<GateResponse> response = createGate(
                TestIamJwtDecoderConfig.ORG_A, STATION_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                new CreateGateRequest("A1", "International"), GateResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().stationId()).isEqualTo(STATION_A);
        assertThat(response.getBody().code()).isEqualTo("A1");
        assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    }

    @Test
    void rejectsUserWithoutGateUpdatePermission() {
        ResponseEntity<String> response = createGate(
                TestIamJwtDecoderConfig.ORG_A, STATION_A, TestIamJwtDecoderConfig.VIEWER_TOKEN,
                new CreateGateRequest("A1", "International"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("MISSING_PERMISSION");
    }

    @Test
    void rejectsPathOrganizationMismatch() {
        ResponseEntity<String> response = createGate(
                TestIamJwtDecoderConfig.ORG_A, STATION_A, TestIamJwtDecoderConfig.OTHER_ORG_ADMIN_TOKEN,
                new CreateGateRequest("A1", "International"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("TENANT_MISMATCH");
    }

    @Test
    void rejectsStationOwnershipMismatchEvenWhenOrgMatchesToken() {
        // Path orgId equals the JWT's organizationId (ORG_A), but STATION_B actually
        // belongs to ORG_B. A naive implementation that only checks path-orgId vs
        // token-orgId (without re-verifying the station's real owner) would let this
        // through and let a tenant attach a gate to another tenant's station.
        ResponseEntity<String> response = createGate(
                TestIamJwtDecoderConfig.ORG_A, STATION_B, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                new CreateGateRequest("A1", "International"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("STATION_NOT_FOUND");

        long gateCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM airport.gates WHERE station_id = ?", Long.class, STATION_B);
        assertThat(gateCount).isZero();
    }

    @Test
    void rejectsDuplicateGateCodeOnSameStation() {
        ResponseEntity<GateResponse> first = createGate(
                TestIamJwtDecoderConfig.ORG_A, STATION_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                new CreateGateRequest("A1", "International"), GateResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> second = createGate(
                TestIamJwtDecoderConfig.ORG_A, STATION_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                new CreateGateRequest("A1", "Domestic"), String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).contains("GATE_CODE_CONFLICT");
    }

    @Test
    void updatesGateStatusThroughValidTransitions() {
        ResponseEntity<GateResponse> created = createGate(
                TestIamJwtDecoderConfig.ORG_A, STATION_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                new CreateGateRequest("A1", "International"), GateResponse.class);
        UUID gateId = created.getBody().id();

        ResponseEntity<GateResponse> maintenance = updateStatus(
                TestIamJwtDecoderConfig.ORG_A, STATION_A, gateId, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                new UpdateGateStatusRequest("MAINTENANCE"));
        assertThat(maintenance.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(maintenance.getBody().status()).isEqualTo("MAINTENANCE");

        ResponseEntity<GateResponse> closed = updateStatus(
                TestIamJwtDecoderConfig.ORG_A, STATION_A, gateId, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                new UpdateGateStatusRequest("CLOSED"));
        assertThat(closed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(closed.getBody().status()).isEqualTo("CLOSED");
    }

    @Test
    void listsGatesForStation() {
        createGate(TestIamJwtDecoderConfig.ORG_A, STATION_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                new CreateGateRequest("A1", "International"), GateResponse.class);
        createGate(TestIamJwtDecoderConfig.ORG_A, STATION_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                new CreateGateRequest("A2", "Domestic"), GateResponse.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestIamJwtDecoderConfig.VIEWER_TOKEN);
        ResponseEntity<GateResponse[]> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/stations/" + STATION_A + "/gates",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                GateResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(response.getBody())).hasSize(2);
    }

    @Test
    void getsSingleGateByIdForAuthorizedReader() {
        ResponseEntity<GateResponse> created = createGate(
                TestIamJwtDecoderConfig.ORG_A, STATION_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                new CreateGateRequest("A1", "International"), GateResponse.class);
        UUID gateId = created.getBody().id();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestIamJwtDecoderConfig.VIEWER_TOKEN);
        ResponseEntity<GateResponse> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/stations/" + STATION_A
                        + "/gates/" + gateId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                GateResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(gateId);
        assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    }

    @Test
    void getSingleGateRejectsStationOwnershipMismatch() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestIamJwtDecoderConfig.ADMIN_TOKEN);
        ResponseEntity<String> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/stations/" + STATION_B
                        + "/gates/" + UUID.randomUUID(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("STATION_NOT_FOUND");
    }

    @Test
    void getsGateByFlatOrganizationLookup() {
        ResponseEntity<GateResponse> created = createGate(
                TestIamJwtDecoderConfig.ORG_A, STATION_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                new CreateGateRequest("A1", "International"), GateResponse.class);
        UUID gateId = created.getBody().id();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestIamJwtDecoderConfig.VIEWER_TOKEN);
        ResponseEntity<GateResponse> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/gates/" + gateId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                GateResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(gateId);
        assertThat(response.getBody().stationId()).isEqualTo(STATION_A);
        assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    }

    @Test
    void flatGateLookupRejectsGateFromAnotherOrganization() {
        ResponseEntity<GateResponse> created = createGate(
                TestIamJwtDecoderConfig.ORG_B, STATION_B, TestIamJwtDecoderConfig.OTHER_ORG_ADMIN_TOKEN,
                new CreateGateRequest("B1", "Domestic"), GateResponse.class);
        UUID gateId = created.getBody().id();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestIamJwtDecoderConfig.ADMIN_TOKEN);
        ResponseEntity<String> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/gates/" + gateId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("GATE_NOT_FOUND");
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/stations/" + STATION_A + "/gates",
                new CreateGateRequest("A1", "International"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private <T> ResponseEntity<T> createGate(
            UUID orgId, UUID stationId, String token, CreateGateRequest request, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                "/organizations/" + orgId + "/stations/" + stationId + "/gates",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                responseType);
    }

    private ResponseEntity<GateResponse> updateStatus(
            UUID orgId, UUID stationId, UUID gateId, String token, UpdateGateStatusRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                "/organizations/" + orgId + "/stations/" + stationId + "/gates/" + gateId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                GateResponse.class);
    }
}
