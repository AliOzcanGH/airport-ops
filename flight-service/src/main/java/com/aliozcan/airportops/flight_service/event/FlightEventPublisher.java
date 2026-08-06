package com.aliozcan.airportops.flight_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes domain events to Kafka only after the originating DB transaction has
 * committed, so a consumer never observes an event for a row it can't yet read.
 */
@Component
public class FlightEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlightEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public FlightEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.flight-events-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFlightCreated(FlightCreatedEvent event) {
        publish("FlightCreated", event.organizationId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFlightStatusChanged(FlightStatusChangedEvent event) {
        publish("FlightStatusChanged", event.organizationId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCompleted(TaskCompletedEvent event) {
        publish("TaskCompleted", event.organizationId(), event);
    }

    private void publish(String eventType, UUID organizationId, Object payload) {
        FlightEventEnvelope envelope = new FlightEventEnvelope(
                UUID.randomUUID(), eventType, organizationId, Instant.now(), payload);
        try {
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(topic, organizationId.toString(), json);
        } catch (JsonProcessingException exception) {
            LOGGER.error(
                    "Failed to serialize {} event for organization {}",
                    eventType, organizationId, exception);
        }
    }
}
