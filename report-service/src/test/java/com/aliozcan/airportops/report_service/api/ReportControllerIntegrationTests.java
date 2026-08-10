package com.aliozcan.airportops.report_service.api;

import com.aliozcan.airportops.report_service.testsupport.TestIamJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.sql.Date;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import(TestIamJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = "flight-events")
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@DirtiesContext
class ReportControllerIntegrationTests {

    private static final Date SUMMARY_DATE = Date.valueOf("2026-08-01");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void tenantUserSeesDailyFlightSummaryForTheirOrganization() {
        seedDailySummary(TestIamJwtDecoderConfig.ORG_A, 5, 2, 1);

        ResponseEntity<Map> response = get(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/reports/daily-flights?date=2026-08-01",
                TestIamJwtDecoderConfig.ORG_A_ADMIN_TOKEN, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalFlights")).isEqualTo(5);
        assertThat(response.getBody().get("delayedFlights")).isEqualTo(2);
        assertThat(response.getBody().get("cancelledFlights")).isEqualTo(1);
    }

    @Test
    void dailyFlightSummaryWithNoDataYetReturnsZeroes() {
        ResponseEntity<Map> response = get(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/reports/daily-flights?date=2026-08-02",
                TestIamJwtDecoderConfig.ORG_A_ADMIN_TOKEN, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalFlights")).isEqualTo(0);
    }

    @Test
    void tenantUserWithoutReportReadPermissionIsRejected() {
        ResponseEntity<Map> response = get(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/reports/daily-flights?date=2026-08-01",
                TestIamJwtDecoderConfig.ORG_A_VIEWER_NO_PERMISSION_TOKEN, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("errorCode")).isEqualTo("MISSING_PERMISSION");
    }

    @Test
    void tenantUserCannotReadAnotherOrganizationsReportViaPathOrgId() {
        ResponseEntity<Map> response = get(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_B + "/reports/daily-flights?date=2026-08-01",
                TestIamJwtDecoderConfig.ORG_A_ADMIN_TOKEN, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("errorCode")).isEqualTo("TENANT_MISMATCH");
    }

    @Test
    void missingTokenIsRejected() {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Map> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/reports/daily-flights?date=2026-08-01",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void gateUtilizationIsCachedOnFirstReadAndInvalidatedByANewEvent() {
        UUID organizationId = TestIamJwtDecoderConfig.ORG_A;
        UUID gateId = UUID.randomUUID();
        String cacheKey = "org:" + organizationId + ":report:gate-utilization:2026-08-01";
        stringRedisTemplate.delete(cacheKey);
        jdbcTemplate.update(
                "DELETE FROM report.gate_utilization WHERE organization_id = ? AND summary_date = ?",
                organizationId, SUMMARY_DATE);
        jdbcTemplate.update(
                "INSERT INTO report.gate_utilization (organization_id, gate_id, summary_date, flight_count) "
                        + "VALUES (?, ?, ?, ?)",
                organizationId, gateId, SUMMARY_DATE, 3);

        assertThat(stringRedisTemplate.hasKey(cacheKey)).isFalse();

        ResponseEntity<List> firstResponse = get(
                "/organizations/" + organizationId + "/reports/gate-utilization?date=2026-08-01",
                TestIamJwtDecoderConfig.ORG_A_ADMIN_TOKEN, List.class);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstResponse.getBody()).hasSize(1);

        // First read is a cache miss that populates Redis.
        assertThat(stringRedisTemplate.hasKey(cacheKey)).isTrue();

        // A new FlightCreated event for the same org/date must invalidate the cache key.
        UUID eventId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        kafkaTemplate.send("flight-events", organizationId.toString(),
                flightCreatedJson(eventId, flightId, organizationId, gateId));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(stringRedisTemplate.hasKey(cacheKey)).isFalse());

        ResponseEntity<List> secondResponse = get(
                "/organizations/" + organizationId + "/reports/gate-utilization?date=2026-08-01",
                TestIamJwtDecoderConfig.ORG_A_ADMIN_TOKEN, List.class);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> entry = (Map<String, Object>) secondResponse.getBody().get(0);
        assertThat(entry.get("flightCount")).isEqualTo(4);
    }

    private String flightCreatedJson(UUID eventId, UUID flightId, UUID organizationId, UUID gateId) {
        return "{"
                + "\"eventId\":\"" + eventId + "\","
                + "\"eventType\":\"FlightCreated\","
                + "\"organizationId\":\"" + organizationId + "\","
                + "\"occurredAt\":\"2026-08-01T10:00:00Z\","
                + "\"payload\":{"
                + "\"flightId\":\"" + flightId + "\","
                + "\"flightNumber\":\"PC202\","
                + "\"status\":\"SCHEDULED\","
                + "\"assignedGateId\":\"" + gateId + "\""
                + "}"
                + "}";
    }

    private void seedDailySummary(UUID organizationId, int total, int delayed, int cancelled) {
        jdbcTemplate.update(
                "INSERT INTO report.daily_flight_summary "
                        + "(organization_id, summary_date, total_flights, delayed_flights, cancelled_flights) "
                        + "VALUES (?, ?, ?, ?, ?) "
                        + "ON CONFLICT (organization_id, summary_date) "
                        + "DO UPDATE SET total_flights = excluded.total_flights, "
                        + "delayed_flights = excluded.delayed_flights, "
                        + "cancelled_flights = excluded.cancelled_flights",
                organizationId, SUMMARY_DATE, total, delayed, cancelled);
    }

    private <T> ResponseEntity<T> get(String path, String token, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }
}
