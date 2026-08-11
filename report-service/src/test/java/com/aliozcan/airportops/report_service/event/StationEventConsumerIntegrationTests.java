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
@EmbeddedKafka(partitions = 1, topics = "station-events")
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@DirtiesContext
class StationEventConsumerIntegrationTests {

    private static final String TOPIC = "station-events";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void stationCreatedEventIncrementsStationCount() {
        UUID eventId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        kafkaTemplate.send(TOPIC, organizationId.toString(), stationCreatedJson(eventId, organizationId));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(stationCount(organizationId)).isEqualTo(1));
    }

    @Test
    void duplicateDeliveryDoesNotDoubleCountStationCount() {
        UUID eventId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String message = stationCreatedJson(eventId, organizationId);

        kafkaTemplate.send(TOPIC, organizationId.toString(), message);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(stationCount(organizationId)).isEqualTo(1));

        kafkaTemplate.send(TOPIC, organizationId.toString(), message);

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(stationCount(organizationId)).isEqualTo(1));
    }

    @Test
    void stationCountsAreIsolatedPerOrganization() {
        UUID organizationA = UUID.randomUUID();
        UUID organizationB = UUID.randomUUID();

        kafkaTemplate.send(TOPIC, organizationA.toString(),
                stationCreatedJson(UUID.randomUUID(), organizationA));
        kafkaTemplate.send(TOPIC, organizationA.toString(),
                stationCreatedJson(UUID.randomUUID(), organizationA));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(stationCount(organizationA)).isEqualTo(2));

        assertThat(stationCount(organizationB)).isEqualTo(0);
    }

    private Integer stationCount(UUID organizationId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE((SELECT station_count FROM report.organization_operational_summary "
                        + "WHERE organization_id = ?), 0)",
                Integer.class, organizationId);
    }

    private String stationCreatedJson(UUID eventId, UUID organizationId) {
        return "{"
                + "\"eventId\":\"" + eventId + "\","
                + "\"eventType\":\"StationCreated\","
                + "\"organizationId\":\"" + organizationId + "\","
                + "\"occurredAt\":\"" + Instant.parse("2026-08-05T10:00:00Z") + "\","
                + "\"payload\":{"
                + "\"stationId\":\"" + UUID.randomUUID() + "\","
                + "\"stationName\":\"SAW Station\","
                + "\"gateCount\":6"
                + "}"
                + "}";
    }
}
