package com.aliozcan.airportops.audit_service.event;

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
    void consumesFlightCreatedEventAndWritesAuditLog() {
        UUID eventId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        kafkaTemplate.send(TOPIC, organizationId.toString(),
                flightCreatedEnvelope(eventId, flightId, organizationId));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM audit.audit_logs WHERE resource_id = ?",
                    Integer.class, flightId);
            assertThat(count).isEqualTo(1);
        });

        String action = jdbcTemplate.queryForObject(
                "SELECT action FROM audit.audit_logs WHERE resource_id = ?",
                String.class, flightId);
        assertThat(action).isEqualTo("FLIGHT_CREATED");

        String resourceType = jdbcTemplate.queryForObject(
                "SELECT resource_type FROM audit.audit_logs WHERE resource_id = ?",
                String.class, flightId);
        assertThat(resourceType).isEqualTo("FLIGHT");

        Integer processedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit.processed_events WHERE event_id = ?",
                Integer.class, eventId);
        assertThat(processedCount).isEqualTo(1);
    }

    @Test
    void consumesTaskCompletedEventAsTaskResource() {
        UUID eventId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        kafkaTemplate.send(TOPIC, organizationId.toString(),
                taskCompletedEnvelope(eventId, taskId, flightId, organizationId));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            String action = jdbcTemplate.queryForObject(
                    "SELECT action FROM audit.audit_logs WHERE resource_id = ?",
                    String.class, taskId);
            assertThat(action).isEqualTo("TASK_COMPLETED");
        });

        String resourceType = jdbcTemplate.queryForObject(
                "SELECT resource_type FROM audit.audit_logs WHERE resource_id = ?",
                String.class, taskId);
        assertThat(resourceType).isEqualTo("TASK");
    }

    @Test
    void duplicateDeliveryOfTheSameEventIsIdempotent() {
        UUID eventId = UUID.randomUUID();
        UUID flightId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String message = flightCreatedEnvelope(eventId, flightId, organizationId);

        kafkaTemplate.send(TOPIC, organizationId.toString(), message);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer processedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM audit.processed_events WHERE event_id = ?",
                    Integer.class, eventId);
            assertThat(processedCount).isEqualTo(1);
        });

        // Same eventId delivered a second time, simulating Kafka's at-least-once
        // redelivery guarantee.
        kafkaTemplate.send(TOPIC, organizationId.toString(), message);

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer processedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM audit.processed_events WHERE event_id = ?",
                    Integer.class, eventId);
            assertThat(processedCount).isEqualTo(1);

            Integer logCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM audit.audit_logs WHERE resource_id = ?",
                    Integer.class, flightId);
            assertThat(logCount).isEqualTo(1);
        });
    }

    private String flightCreatedEnvelope(UUID eventId, UUID flightId, UUID organizationId) {
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

    private String taskCompletedEnvelope(UUID eventId, UUID taskId, UUID flightId, UUID organizationId) {
        return "{"
                + "\"eventId\":\"" + eventId + "\","
                + "\"eventType\":\"TaskCompleted\","
                + "\"organizationId\":\"" + organizationId + "\","
                + "\"occurredAt\":\"" + Instant.parse("2026-08-05T10:00:00Z") + "\","
                + "\"payload\":{"
                + "\"taskId\":\"" + taskId + "\","
                + "\"flightId\":\"" + flightId + "\","
                + "\"taskType\":\"CLEANING\""
                + "}"
                + "}";
    }
}
