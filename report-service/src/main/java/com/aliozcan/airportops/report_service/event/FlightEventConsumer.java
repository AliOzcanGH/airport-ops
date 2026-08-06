package com.aliozcan.airportops.report_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumes flight-events and records a minimal report entry per event, keyed by
 * eventId so at-least-once redelivery from Kafka never produces duplicate rows.
 */
@Component
public class FlightEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlightEventConsumer.class);

    private final ProcessedEventRepository processedEventRepository;
    private final FlightReportEntryRepository flightReportEntryRepository;
    private final ObjectMapper objectMapper;

    public FlightEventConsumer(
            ProcessedEventRepository processedEventRepository,
            FlightReportEntryRepository flightReportEntryRepository,
            ObjectMapper objectMapper) {
        this.processedEventRepository = processedEventRepository;
        this.flightReportEntryRepository = flightReportEntryRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.flight-events-topic}")
    @Transactional
    public void onMessage(String message) {
        FlightEventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message, FlightEventEnvelope.class);
        } catch (JsonProcessingException exception) {
            LOGGER.error("Failed to parse flight event message: {}", message, exception);
            return;
        }

        if (processedEventRepository.existsById(envelope.eventId())) {
            LOGGER.info("Event {} already processed, skipping", envelope.eventId());
            return;
        }
        processedEventRepository.save(new ProcessedEventEntity(envelope.eventId()));

        flightReportEntryRepository.save(new FlightReportEntryEntity(
                extractFlightId(envelope.payload()),
                envelope.organizationId(),
                envelope.eventType(),
                Instant.parse(envelope.occurredAt())));
    }

    private UUID extractFlightId(JsonNode payload) {
        if (payload == null || !payload.hasNonNull("flightId")) {
            return null;
        }
        return UUID.fromString(payload.get("flightId").asText());
    }
}
