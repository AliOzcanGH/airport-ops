package com.aliozcan.airportops.airport_service.event;

import com.aliozcan.airportops.airport_service.station.dto.CreateStationRequest;
import com.aliozcan.airportops.airport_service.station.dto.StationResponse;
import com.aliozcan.airportops.airport_service.testsupport.TestIamJwtDecoderConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestIamJwtDecoderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = "station-events")
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@DirtiesContext
class StationEventPublishingIntegrationTests {

    private static final String TOPIC = "station-events";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUpConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "station-events-test-" + UUID.randomUUID(), "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new KafkaConsumer<>(consumerProps);
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, TOPIC);
    }

    @AfterEach
    void tearDownConsumer() {
        consumer.close();
    }

    @Test
    void publishesStationCreatedEventAfterStationIsCommitted() {
        String stationName = "Station " + UUID.randomUUID().toString().substring(0, 8);

        StationResponse created = createStation(TestIamJwtDecoderConfig.ORG_A,
                TestIamJwtDecoderConfig.ADMIN_TOKEN, stationName);

        JsonNode envelope = findEnvelope("StationCreated", stationName);
        assertThat(envelope.get("eventId").asText()).isNotBlank();
        assertThat(envelope.get("organizationId").asText()).isEqualTo(TestIamJwtDecoderConfig.ORG_A.toString());
        assertThat(envelope.get("occurredAt").asText()).isNotBlank();
        JsonNode payload = envelope.get("payload");
        assertThat(payload.get("stationId").asText()).isEqualTo(created.id().toString());
        assertThat(payload.get("gateCount").asInt()).isEqualTo(6);
    }

    @Test
    void stationCreatedEventIsKeyedByOrganizationId() {
        String stationName = "Station " + UUID.randomUUID().toString().substring(0, 8);

        createStation(TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN, stationName);

        ConsumerRecord<String, String> record = findRecord("StationCreated", stationName);
        assertThat(record.key()).isEqualTo(TestIamJwtDecoderConfig.ORG_A.toString());
    }

    private StationResponse createStation(UUID organizationId, String token, String stationName) {
        CreateStationRequest request = new CreateStationRequest(stationName, "SAW", 6);
        ResponseEntity<StationResponse> response = restTemplate.exchange(
                "/organizations/" + organizationId + "/stations",
                HttpMethod.POST,
                new HttpEntity<>(request, bearerHeaders(token)),
                StationResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private JsonNode findEnvelope(String eventType, String needle) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            var records = consumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, String> record : records) {
                JsonNode envelope = parse(record.value());
                if (eventType.equals(envelope.get("eventType").asText())
                        && envelope.get("payload").toString().contains(needle)) {
                    return envelope;
                }
            }
        }
        throw new AssertionError("No event of type " + eventType + " containing " + needle + " was published");
    }

    private ConsumerRecord<String, String> findRecord(String eventType, String needle) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            var records = consumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, String> record : records) {
                JsonNode envelope = parse(record.value());
                if (eventType.equals(envelope.get("eventType").asText())
                        && envelope.get("payload").toString().contains(needle)) {
                    return record;
                }
            }
        }
        throw new AssertionError("No record of type " + eventType + " containing " + needle + " was published");
    }

    private JsonNode parse(String rawValue) {
        try {
            return OBJECT_MAPPER.readTree(rawValue);
        } catch (Exception exception) {
            throw new AssertionError("Could not parse event payload: " + rawValue, exception);
        }
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
