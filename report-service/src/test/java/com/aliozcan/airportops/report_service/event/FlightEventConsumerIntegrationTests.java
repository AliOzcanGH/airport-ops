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

    private String envelopeJson(UUID eventId, UUID flightId, UUID organizationId) {
        return "{"
                + "\"eventId\":\"" + eventId + "\","
                + "\"eventType\":\"FlightCreated\","
                + "\"organizationId\":\"" + organizationId + "\","
                + "\"occurredAt\":\"" + Instant.parse("2026-08-05T10:00:00Z") + "\","
                + "\"payload\":{"
                + "\"flightId\":\"" + flightId + "\","
                + "\"flightNumber\":\"PC101\","
                + "\"status\":\"SCHEDULED\""
                + "}"
                + "}";
    }
}
