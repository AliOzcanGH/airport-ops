package com.aliozcan.airportops.audit_service.event;

import com.aliozcan.airportops.audit_service.domain.AuditLogEntity;
import com.aliozcan.airportops.audit_service.domain.AuditLogRepository;
import com.aliozcan.airportops.audit_service.domain.ProcessedEventEntity;
import com.aliozcan.airportops.audit_service.domain.ProcessedEventRepository;
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
 * Consumes flight-events (independently of report-service's own consumer
 * group on the same topic) and records one audit_logs row per event, keyed
 * by eventId so at-least-once redelivery never produces duplicate rows.
 */
@Component
public class FlightEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlightEventConsumer.class);

    private final ProcessedEventRepository processedEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public FlightEventConsumer(
            ProcessedEventRepository processedEventRepository,
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper) {
        this.processedEventRepository = processedEventRepository;
        this.auditLogRepository = auditLogRepository;
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
        processedEventRepository.save(ProcessedEventEntity.of(envelope.eventId()));

        String action = actionFor(envelope.eventType());
        String resourceType = resourceTypeFor(envelope.eventType());
        if (action == null || resourceType == null) {
            LOGGER.warn("Unrecognized flight event type '{}', recording no audit row", envelope.eventType());
            return;
        }

        auditLogRepository.save(AuditLogEntity.record(
                envelope.organizationId(),
                null,
                null,
                action,
                resourceType,
                resourceIdFor(resourceType, envelope.payload()),
                Instant.parse(envelope.occurredAt()),
                envelope.payload() == null ? null : envelope.payload().toString()));
    }

    private String actionFor(String eventType) {
        return switch (eventType) {
            case "FlightCreated" -> "FLIGHT_CREATED";
            case "FlightStatusChanged" -> "FLIGHT_STATUS_CHANGED";
            case "TaskCompleted" -> "TASK_COMPLETED";
            default -> null;
        };
    }

    private String resourceTypeFor(String eventType) {
        return switch (eventType) {
            case "FlightCreated", "FlightStatusChanged" -> "FLIGHT";
            case "TaskCompleted" -> "TASK";
            default -> null;
        };
    }

    private UUID resourceIdFor(String resourceType, JsonNode payload) {
        if (payload == null) {
            return null;
        }
        String field = "TASK".equals(resourceType) ? "taskId" : "flightId";
        if (!payload.hasNonNull(field)) {
            return null;
        }
        return UUID.fromString(payload.get(field).asText());
    }
}
