package com.aliozcan.airportops.flight_service.flight;

import com.aliozcan.airportops.flight_service.flight.dto.CreateFlightRequest;
import com.aliozcan.airportops.flight_service.testsupport.TestIamJwtDecoderConfig;
import com.aliozcan.airportops.flight_service.testsupport.UnavailableAirportServiceConfig;
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

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * W10 acceptance criterion: airport-service being completely unreachable must
 * produce an explicit, well-defined error — never a silent "gate is valid"
 * fallback. Points the airport-service client at a closed local port so the
 * RestClient call fails with a real connection-refused error, not a stub.
 */
@Import({TestIamJwtDecoderConfig.class, UnavailableAirportServiceConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FlightCreateAirportServiceUnavailableTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsGateVerificationUnavailableWhenAirportServiceIsUnreachable() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestIamJwtDecoderConfig.ADMIN_TOKEN);
        CreateFlightRequest request = new CreateFlightRequest(
                "PC101", "SAW", "IST",
                Instant.parse("2026-09-01T10:00:00Z"), Instant.parse("2026-09-01T11:00:00Z"),
                UUID.randomUUID());

        ResponseEntity<String> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/flights",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).contains("GATE_VERIFICATION_UNAVAILABLE");
    }
}
