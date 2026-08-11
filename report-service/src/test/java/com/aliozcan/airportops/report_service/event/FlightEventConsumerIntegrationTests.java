package com.aliozcan.airportops.report_service.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "flight-events")
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@DirtiesContext
class FlightEventConsumerIntegrationTests {

    private static final String TOPIC = "flight-events";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void consumesFlightCreatedEventAndWritesReportEntry() {
        UUID eventId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        kafkaTemplate.send(TOPIC, organizationId.toString(), envelopeJson(eventId, flightId, organizationId));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer entryCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM report.flight_report_entries WHERE flight_id = ?",
                    Integer.class, flightId);
            assertThat(entryCount).isEqualTo(1);
        });

        Integer processedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM report.processed_events WHERE event_id = ?",
                Integer.class, eventId);
        assertThat(processedCount).isEqualTo(1);

        String eventType = jdbcTemplate.queryForObject(
                "SELECT event_type FROM report.flight_report_entries WHERE flight_id = ?",
                String.class, flightId);
        assertThat(eventType).isEqualTo("FlightCreated");
    }

    @Test
    void duplicateDeliveryOfTheSameEventIsIdempotent() {
        UUID eventId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String message = envelopeJson(eventId, flightId, organizationId);

        kafkaTemplate.send(TOPIC, organizationId.toString(), message);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer processedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM report.processed_events WHERE event_id = ?",
                    Integer.class, eventId);
            assertThat(processedCount).isEqualTo(1);
        });

        // Same eventId delivered a second time, simulating Kafka's at-least-once
        // redelivery guarantee.
        kafkaTemplate.send(TOPIC, organizationId.toString(), message);

        // Give the second delivery time to be (not) processed, then assert exactly
        // one row exists in both tables — the unique eventId must have blocked it.
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer processedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM report.processed_events WHERE event_id = ?",
                    Integer.class, eventId);
            assertThat(processedCount).isEqualTo(1);

            Integer entryCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM report.flight_report_entries WHERE flight_id = ?",
                    Integer.class, flightId);
            assertThat(entryCount).isEqualTo(1);
        });
    }

    @Test
    void flightCreatedEventIncrementsDailyTotalAndGateUtilization() {
        UUID eventId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID gateId = UUID.randomUUID();

        kafkaTemplate.send(TOPIC, organizationId.toString(),
                flightCreatedJson(eventId, flightId, organizationId, gateId));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer totalFlights = totalFlights(organizationId);
            assertThat(totalFlights).isEqualTo(1);
        });

        Integer gateFlightCount = gateFlightCount(organizationId, gateId);
        assertThat(gateFlightCount).isEqualTo(1);
    }

    @Test
    void flightCreatedEventIncrementsOrganizationOperationalSummary() {
        UUID eventId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        kafkaTemplate.send(TOPIC, organizationId.toString(),
                flightCreatedJson(eventId, flightId, organizationId, UUID.randomUUID()));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer totalFlightsLast30Days = jdbcTemplate.queryForObject(
                    "SELECT total_flights_last_30_days FROM report.organization_operational_summary "
                            + "WHERE organization_id = ?",
                    Integer.class, organizationId);
            assertThat(totalFlightsLast30Days).isEqualTo(1);

            java.sql.Timestamp lastFlightActivityAt = jdbcTemplate.queryForObject(
                    "SELECT last_flight_activity_at FROM report.organization_operational_summary "
                            + "WHERE organization_id = ?",
                    java.sql.Timestamp.class, organizationId);
            assertThat(lastFlightActivityAt.toInstant()).isEqualTo(Instant.parse("2026-08-05T10:00:00Z"));
        });
    }

    @Test
    void flightStatusChangedToDelayedIncrementsDelayedFlightsCount() {
        UUID eventId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        kafkaTemplate.send(TOPIC, organizationId.toString(),
                statusChangedJson(eventId, flightId, organizationId, "DELAYED"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer delayedFlights = delayedFlights(organizationId);
            assertThat(delayedFlights).isEqualTo(1);
        });
    }

    /**
     * The plan's most critical test: redelivery of an already-processed event must
     * not double-count the read model, proving the W13 idempotency guarantee
     * (processed_events check) extends to the new read-model writes too.
     */
    @Test
    void duplicateDeliveryDoesNotDoubleCountTheReadModel() {
        UUID eventId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID gateId = UUID.randomUUID();
        String message = flightCreatedJson(eventId, flightId, organizationId, gateId);

        kafkaTemplate.send(TOPIC, organizationId.toString(), message);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer totalFlights = totalFlights(organizationId);
            assertThat(totalFlights).isEqualTo(1);
        });

        // Redeliver the exact same event, simulating Kafka's at-least-once guarantee.
        kafkaTemplate.send(TOPIC, organizationId.toString(), message);

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(totalFlights(organizationId)).isEqualTo(1);
            assertThat(gateFlightCount(organizationId, gateId)).isEqualTo(1);
        });
    }

    private Integer totalFlights(UUID organizationId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE((SELECT total_flights FROM report.daily_flight_summary "
                        + "WHERE organization_id = ? AND summary_date = ?), 0)",
                Integer.class, organizationId, java.sql.Date.valueOf("2026-08-05"));
    }

    private Integer delayedFlights(UUID organizationId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE((SELECT delayed_flights FROM report.daily_flight_summary "
                        + "WHERE organization_id = ? AND summary_date = ?), 0)",
                Integer.class, organizationId, java.sql.Date.valueOf("2026-08-05"));
    }

    private Integer gateFlightCount(UUID organizationId, UUID gateId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE((SELECT flight_count FROM report.gate_utilization "
                        + "WHERE organization_id = ? AND gate_id = ? AND summary_date = ?), 0)",
                Integer.class, organizationId, gateId, java.sql.Date.valueOf("2026-08-05"));
    }

    private String envelopeJson(UUID eventId, UUID flightId, UUID organizationId) {
        return flightCreatedJson(eventId, flightId, organizationId, null);
    }

    private String flightCreatedJson(UUID eventId, UUID flightId, UUID organizationId, UUID gateId) {
        String gateField = gateId == null ? "" : ",\"assignedGateId\":\"" + gateId + "\"";
        return "{"
                + "\"eventId\":\"" + eventId + "\","
                + "\"eventType\":\"FlightCreated\","
                + "\"organizationId\":\"" + organizationId + "\","
                + "\"occurredAt\":\"" + Instant.parse("2026-08-05T10:00:00Z") + "\","
                + "\"payload\":{"
                + "\"flightId\":\"" + flightId + "\","
                + "\"flightNumber\":\"PC101\","
                + "\"status\":\"SCHEDULED\""
                + gateField
                + "}"
                + "}";
    }

    private String statusChangedJson(UUID eventId, UUID flightId, UUID organizationId, String newStatus) {
        return "{"
                + "\"eventId\":\"" + eventId + "\","
                + "\"eventType\":\"FlightStatusChanged\","
                + "\"organizationId\":\"" + organizationId + "\","
                + "\"occurredAt\":\"" + Instant.parse("2026-08-05T10:00:00Z") + "\","
                + "\"payload\":{"
                + "\"flightId\":\"" + flightId + "\","
                + "\"flightNumber\":\"PC101\","
                + "\"previousStatus\":\"SCHEDULED\","
                + "\"newStatus\":\"" + newStatus + "\""
                + "}"
                + "}";
    }
}
