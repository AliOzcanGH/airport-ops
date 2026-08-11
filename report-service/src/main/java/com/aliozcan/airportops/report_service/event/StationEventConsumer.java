package com.aliozcan.airportops.report_service.event;

import com.aliozcan.airportops.report_service.readmodel.OrganizationOperationalSummaryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes station-events and folds StationCreated into the per-organization
 * operational summary read model. Idempotency is enforced the same way as
 * {@link FlightEventConsumer}: the eventId is recorded before the read model
 * is updated, so redelivery can never double count.
 */
@Component
public class StationEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(StationEventConsumer.class);

    private final ProcessedEventRepository processedEventRepository;
    private final OrganizationOperationalSummaryRepository operationalSummaryRepository;
    private final ObjectMapper objectMapper;

    public StationEventConsumer(
            ProcessedEventRepository processedEventRepository,
            OrganizationOperationalSummaryRepository operationalSummaryRepository,
            ObjectMapper objectMapper) {
        this.processedEventRepository = processedEventRepository;
        this.operationalSummaryRepository = operationalSummaryRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.station-events-topic}")
    @Transactional
    public void onMessage(String message) {
        StationEventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message, StationEventEnvelope.class);
        } catch (JsonProcessingException exception) {
            LOGGER.error("Failed to parse station event message: {}", message, exception);
            return;
        }

        if (processedEventRepository.existsById(envelope.eventId())) {
            LOGGER.info("Event {} already processed, skipping", envelope.eventId());
            return;
        }
        processedEventRepository.save(new ProcessedEventEntity(envelope.eventId()));

        if ("StationCreated".equals(envelope.eventType())) {
            operationalSummaryRepository.incrementStationCount(envelope.organizationId());
        }
    }
}
