package com.aliozcan.airportops.audit_service.internal;

import com.aliozcan.airportops.audit_service.domain.AuditLogEntity;
import com.aliozcan.airportops.audit_service.domain.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Direct (non-Kafka) audit write path for critical actions in other services
 * — e.g. iam-service posting here after a role-update transaction commits.
 * Protected by {@link com.aliozcan.airportops.audit_service.config.InternalServiceSecretFilter},
 * not end-user JWT auth.
 */
@RestController
@RequestMapping("/internal/audit-logs")
public class InternalAuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public InternalAuditLogController(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody CreateAuditLogRequest request) {
        String metadataJson;
        try {
            metadataJson = request.metadata() == null ? null : objectMapper.writeValueAsString(request.metadata());
        } catch (Exception exception) {
            metadataJson = null;
        }
        auditLogRepository.save(AuditLogEntity.record(
                request.organizationId(),
                request.actorUserId(),
                request.actorEmail(),
                request.action(),
                request.resourceType(),
                request.resourceId(),
                request.occurredAt(),
                metadataJson));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
