package com.aliozcan.airportops.report_service.event;

import com.aliozcan.airportops.report_service.cache.GateUtilizationCache;
import com.aliozcan.airportops.report_service.readmodel.DailyFlightSummaryRepository;
import com.aliozcan.airportops.report_service.readmodel.GateUtilizationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Consumes flight-events, records a minimal report entry per event (keyed by
 * eventId so at-least-once redelivery from Kafka never produces duplicate
 * rows), and folds the event into the daily-flight-summary / gate-utilization
 * read models. The read-model update happens only after the idempotency
 * record is written, so a redelivered event can never double-count.
 */
@Component
public class FlightEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlightEventConsumer.class);

    private final ProcessedEventRepository processedEventRepository;
    private final FlightReportEntryRepository flightReportEntryRepository;
    private final DailyFlightSummaryRepository dailyFlightSummaryRepository;
    private final GateUtilizationRepository gateUtilizationRepository;
    private final GateUtilizationCache gateUtilizationCache;
    private final ObjectMapper objectMapper;

    public FlightEventConsumer(
            ProcessedEventRepository processedEventRepository,
            FlightReportEntryRepository flightReportEntryRepository,
            DailyFlightSummaryRepository dailyFlightSummaryRepository,
            GateUtilizationRepository gateUtilizationRepository,
            GateUtilizationCache gateUtilizationCache,
            ObjectMapper objectMapper) {
        this.processedEventRepository = processedEventRepository;
        this.flightReportEntryRepository = flightReportEntryRepository;
        this.dailyFlightSummaryRepository = dailyFlightSummaryRepository;
        this.gateUtilizationRepository = gateUtilizationRepository;
        this.gateUtilizationCache = gateUtilizationCache;
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

        updateReadModel(envelope);
    }

    private void updateReadModel(FlightEventEnvelope envelope) {
        UUID organizationId = envelope.organizationId();
        LocalDate summaryDate = Instant.parse(envelope.occurredAt()).atZone(ZoneOffset.UTC).toLocalDate();

        switch (envelope.eventType()) {
            case "FlightCreated" -> {
                dailyFlightSummaryRepository.incrementTotalFlights(organizationId, summaryDate);
                UUID assignedGateId = extractUuid(envelope.payload(), "assignedGateId");
                if (assignedGateId != null) {
                    gateUtilizationRepository.incrementFlightCount(organizationId, assignedGateId, summaryDate);
                    gateUtilizationCache.evict(organizationId, summaryDate);
                }
            }
            case "FlightStatusChanged" -> {
                String newStatus = envelope.payload() == null ? null
                        : envelope.payload().path("newStatus").asText(null);
                if ("DELAYED".equals(newStatus)) {
                    dailyFlightSummaryRepository.incrementDelayedFlights(organizationId, summaryDate);
                } else if ("CANCELLED".equals(newStatus)) {
                    dailyFlightSummaryRepository.incrementCancelledFlights(organizationId, summaryDate);
                }
                gateUtilizationCache.evict(organizationId, summaryDate);
            }
            default -> {
                // No read-model impact for other event types.
            }
        }
    }

    private UUID extractFlightId(JsonNode payload) {
        return extractUuid(payload, "flightId");
    }

    private UUID extractUuid(JsonNode payload, String fieldName) {
        if (payload == null || !payload.hasNonNull(fieldName)) {
            return null;
        }
        return UUID.fromString(payload.get(fieldName).asText());
    }
}
