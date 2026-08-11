package com.aliozcan.airportops.airport_service.event;

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
public class StationEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(StationEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public StationEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.station-events-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStationCreated(StationCreatedEvent event) {
        publish("StationCreated", event.organizationId(), event);
    }

    private void publish(String eventType, UUID organizationId, Object payload) {
        StationEventEnvelope envelope = new StationEventEnvelope(
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
