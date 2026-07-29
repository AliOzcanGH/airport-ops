package com.aliozcan.airportops.airport_service.station;

import com.aliozcan.airportops.airport_service.station.dto.CreateStationRequest;
import com.aliozcan.airportops.airport_service.station.dto.StationResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestIamJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StationControllerIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createsStationForAuthorizedAdmin() {
        CreateStationRequest request = new CreateStationRequest("SAW Station", "SAW", 8);

        ResponseEntity<StationResponse> response = createStation(
                TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                request, StationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().organizationId()).isEqualTo(TestIamJwtDecoderConfig.ORG_A);
        assertThat(response.getBody().stationName()).isEqualTo("SAW Station");
        assertThat(response.getBody().gateCount()).isEqualTo(8);
    }

    @Test
    void rejectsUserWithoutStationCreatePermission() {
        CreateStationRequest request = new CreateStationRequest("SAW Station", "SAW", 8);

        ResponseEntity<String> response = createStation(
                TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.VIEWER_TOKEN,
                request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rejectsPathOrganizationMismatch() {
        CreateStationRequest request = new CreateStationRequest("SAW Station", "SAW", 8);

        ResponseEntity<String> response = createStation(
                TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.OTHER_ORG_ADMIN_TOKEN,
                request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("TENANT_MISMATCH");
    }

    @Test
    void rejectsRequestWithoutBearerToken() {
        CreateStationRequest request = new CreateStationRequest("SAW Station", "SAW", 8);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/stations",
                request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsInvalidBearerToken() {
        CreateStationRequest request = new CreateStationRequest("SAW Station", "SAW", 8);

        ResponseEntity<String> response = createStation(
                TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.INVALID_TOKEN,
                request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private <T> ResponseEntity<T> createStation(
            java.util.UUID orgId, String token, CreateStationRequest request, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                "/organizations/" + orgId + "/stations",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                responseType);
    }
}
