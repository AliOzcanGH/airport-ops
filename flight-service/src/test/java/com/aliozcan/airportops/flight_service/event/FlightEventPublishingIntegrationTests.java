package com.aliozcan.airportops.flight_service.event;

import com.aliozcan.airportops.flight_service.flight.dto.CreateFlightRequest;
import com.aliozcan.airportops.flight_service.flight.dto.FlightResponse;
import com.aliozcan.airportops.flight_service.testsupport.MockAirportServiceConfig;
import com.aliozcan.airportops.flight_service.testsupport.TestIamJwtDecoderConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.client.MockRestServiceServer;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@Import({TestIamJwtDecoderConfig.class, MockAirportServiceConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = "flight-events")
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@DirtiesContext
@Sql(statements = {
        "DELETE FROM flight.turnaround_tasks WHERE flight_id IN "
                + "(SELECT id FROM flight.flights WHERE organization_id IN "
                + "('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222'))",
        "DELETE FROM flight.flights WHERE organization_id IN "
                + "('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222')"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM flight.turnaround_tasks WHERE flight_id IN "
                + "(SELECT id FROM flight.flights WHERE organization_id IN "
                + "('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222'))",
        "DELETE FROM flight.flights WHERE organization_id IN "
                + "('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222')"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class FlightEventPublishingIntegrationTests {

    private static final String TOPIC = "flight-events";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockRestServiceServer mockAirportServiceServer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUpConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "flight-events-test-" + UUID.randomUUID(), "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps);
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, TOPIC);
    }

    @AfterEach
    void tearDownConsumer() {
        consumer.close();
    }

    @Test
    void publishesFlightCreatedEventAfterFlightIsCommitted() {
        UUID gateId = UUID.randomUUID();
        expectGateLookup(TestIamJwtDecoderConfig.ORG_A, gateId, TestIamJwtDecoderConfig.ADMIN_TOKEN);
        String flightNumber = "KE" + UUID.randomUUID().toString().substring(0, 4);

        UUID flightId = createFlight(TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                gateId, flightNumber);

        JsonNode envelope = findEnvelope("FlightCreated", flightNumber);
        assertThat(envelope.get("eventId").asText()).isNotBlank();
        assertThat(envelope.get("organizationId").asText()).isEqualTo(TestIamJwtDecoderConfig.ORG_A.toString());
        assertThat(envelope.get("occurredAt").asText()).isNotBlank();
        JsonNode payload = envelope.get("payload");
        assertThat(payload.get("flightId").asText()).isEqualTo(flightId.toString());
        assertThat(payload.get("status").asText()).isEqualTo("SCHEDULED");
    }

    @Test
    void flightCreatedEventIsKeyedByOrganizationId() {
        UUID gateId = UUID.randomUUID();
        expectGateLookup(TestIamJwtDecoderConfig.ORG_A, gateId, TestIamJwtDecoderConfig.ADMIN_TOKEN);
        String flightNumber = "KE" + UUID.randomUUID().toString().substring(0, 4);

        createFlight(TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN, gateId, flightNumber);

        ConsumerRecord<String, String> record = findRecord("FlightCreated", flightNumber);
        assertThat(record.key()).isEqualTo(TestIamJwtDecoderConfig.ORG_A.toString());
    }

    @Test
    void publishesFlightStatusChangedEventWithOldAndNewStatus() {
        UUID gateId = UUID.randomUUID();
        expectGateLookup(TestIamJwtDecoderConfig.ORG_A, gateId, TestIamJwtDecoderConfig.ADMIN_TOKEN);
        String flightNumber = "KE" + UUID.randomUUID().toString().substring(0, 4);
        UUID flightId = createFlight(TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                gateId, flightNumber);

        restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/flights/" + flightId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("status", "BOARDING"), bearerHeaders(TestIamJwtDecoderConfig.ADMIN_TOKEN)),
                Map.class);

        JsonNode envelope = findEnvelope("FlightStatusChanged", flightNumber);
        JsonNode payload = envelope.get("payload");
        assertThat(payload.get("flightId").asText()).isEqualTo(flightId.toString());
        assertThat(payload.get("previousStatus").asText()).isEqualTo("SCHEDULED");
        assertThat(payload.get("newStatus").asText()).isEqualTo("BOARDING");
    }

    @Test
    void publishesTaskCompletedEventOnlyWhenTaskReachesDone() {
        UUID gateId = UUID.randomUUID();
        expectGateLookup(TestIamJwtDecoderConfig.ORG_A, gateId, TestIamJwtDecoderConfig.ADMIN_TOKEN);
        String flightNumber = "KE" + UUID.randomUUID().toString().substring(0, 4);
        UUID flightId = createFlight(TestIamJwtDecoderConfig.ORG_A, TestIamJwtDecoderConfig.ADMIN_TOKEN,
                gateId, flightNumber);
        UUID taskId = firstTaskId(flightId);

        updateTaskStatus(flightId, taskId, "IN_PROGRESS");
        assertThat(pollForEnvelope("TaskCompleted", taskId.toString(), Duration.ofSeconds(2))).isNull();

        updateTaskStatus(flightId, taskId, "DONE");
        JsonNode envelope = findEnvelope("TaskCompleted", taskId.toString());
        JsonNode payload = envelope.get("payload");
        assertThat(payload.get("taskId").asText()).isEqualTo(taskId.toString());
        assertThat(payload.get("flightId").asText()).isEqualTo(flightId.toString());
        assertThat(envelope.get("organizationId").asText()).isEqualTo(TestIamJwtDecoderConfig.ORG_A.toString());
    }

    private void updateTaskStatus(UUID flightId, UUID taskId, String status) {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/flights/" + flightId
                        + "/tasks/" + taskId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("status", status), bearerHeaders(TestIamJwtDecoderConfig.ADMIN_TOKEN)),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private JsonNode findEnvelope(String eventType, String needle) {
        JsonNode found = pollForEnvelope(eventType, needle, Duration.ofSeconds(10));
        assertThat(found).as("event of type %s containing %s", eventType, needle).isNotNull();
        return found;
    }

    private ConsumerRecord<String, String> findRecord(String eventType, String needle) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            var records = consumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, String> record : records) {
                if (matches(record.value(), eventType, needle)) {
                    return record;
                }
            }
        }
        throw new AssertionError("No record of type " + eventType + " containing " + needle + " was published");
    }

    private JsonNode pollForEnvelope(String eventType, String needle, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            var records = consumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, String> record : records) {
                if (matches(record.value(), eventType, needle)) {
                    return parse(record.value());
                }
            }
        }
        return null;
    }

    private boolean matches(String rawValue, String eventType, String needle) {
        JsonNode envelope = parse(rawValue);
        return eventType.equals(envelope.get("eventType").asText())
                && envelope.get("payload").toString().contains(needle);
    }

    private JsonNode parse(String rawValue) {
        try {
            return OBJECT_MAPPER.readTree(rawValue);
        } catch (Exception exception) {
            throw new AssertionError("Could not parse event payload: " + rawValue, exception);
        }
    }

    private void expectGateLookup(UUID organizationId, UUID gateId, String token) {
        mockAirportServiceServer.expect(requestTo(
                        "http://mock-airport-service/organizations/" + organizationId + "/gates/" + gateId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":\"" + gateId + "\",\"stationId\":\"" + UUID.randomUUID()
                                + "\",\"code\":\"A1\",\"status\":\"ACTIVE\"}"));
    }

    private UUID createFlight(UUID organizationId, String token, UUID gateId, String flightNumber) {
        CreateFlightRequest request = new CreateFlightRequest(
                flightNumber, "SAW", "IST",
                Instant.parse("2026-09-01T10:00:00Z"), Instant.parse("2026-09-01T11:00:00Z"), gateId);

        ResponseEntity<FlightResponse> created = restTemplate.exchange(
                "/organizations/" + organizationId + "/flights",
                HttpMethod.POST,
                new HttpEntity<>(request, bearerHeaders(token)),
                FlightResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().id();
    }

    private UUID firstTaskId(UUID flightId) {
        ResponseEntity<Map[]> response = restTemplate.exchange(
                "/organizations/" + TestIamJwtDecoderConfig.ORG_A + "/flights/" + flightId + "/tasks",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(TestIamJwtDecoderConfig.ADMIN_TOKEN)),
                Map[].class);
        return UUID.fromString((String) response.getBody()[0].get("id"));
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
