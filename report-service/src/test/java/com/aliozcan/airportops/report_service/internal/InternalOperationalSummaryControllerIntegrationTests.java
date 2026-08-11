package com.aliozcan.airportops.report_service.internal;

import com.aliozcan.airportops.report_service.config.InternalServiceSecretFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InternalOperationalSummaryControllerIntegrationTests {

    private static final String CORRECT_SECRET = "local-dev-internal-secret";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsOperationalSummaryWhenInternalSecretIsCorrect() {
        UUID organizationId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO report.organization_operational_summary "
                        + "(organization_id, station_count, total_flights_last_30_days) VALUES (?, ?, ?)",
                organizationId, 3, 12);

        ResponseEntity<OperationalSummaryResponse> response = restTemplate.exchange(
                "/internal/organizations/" + organizationId + "/operational-summary",
                HttpMethod.GET,
                new HttpEntity<>(secretHeaders()),
                OperationalSummaryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().stationCount()).isEqualTo(3);
        assertThat(response.getBody().totalFlightsLast30Days()).isEqualTo(12);
    }

    @Test
    void returnsZeroedSummaryWhenOrganizationHasNoReadModelRowYet() {
        UUID organizationId = UUID.randomUUID();

        ResponseEntity<OperationalSummaryResponse> response = restTemplate.exchange(
                "/internal/organizations/" + organizationId + "/operational-summary",
                HttpMethod.GET,
                new HttpEntity<>(secretHeaders()),
                OperationalSummaryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().stationCount()).isEqualTo(0);
        assertThat(response.getBody().totalFlightsLast30Days()).isEqualTo(0);
    }

    @Test
    void operationalSummaryOfOneOrganizationNeverLeaksIntoAnother() {
        UUID organizationA = UUID.randomUUID();
        UUID organizationB = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO report.organization_operational_summary "
                        + "(organization_id, station_count, total_flights_last_30_days) VALUES (?, ?, ?)",
                organizationA, 5, 20);

        ResponseEntity<OperationalSummaryResponse> response = restTemplate.exchange(
                "/internal/organizations/" + organizationB + "/operational-summary",
                HttpMethod.GET,
                new HttpEntity<>(secretHeaders()),
                OperationalSummaryResponse.class);

        assertThat(response.getBody().stationCount()).isEqualTo(0);
        assertThat(response.getBody().totalFlightsLast30Days()).isEqualTo(0);
    }

    @Test
    void rejectsRequestWithoutInternalSecretHeader() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/internal/organizations/" + UUID.randomUUID() + "/operational-summary",
                HttpMethod.GET, HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsRequestWithWrongInternalSecretHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(InternalServiceSecretFilter.HEADER_NAME, "wrong-secret");

        ResponseEntity<String> response = restTemplate.exchange(
                "/internal/organizations/" + UUID.randomUUID() + "/operational-summary",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpHeaders secretHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(InternalServiceSecretFilter.HEADER_NAME, CORRECT_SECRET);
        return headers;
    }
}
