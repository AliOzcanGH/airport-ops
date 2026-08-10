package com.aliozcan.airportops.audit_service.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    List<AuditLogEntity> findByOrganizationIdOrderByOccurredAtDesc(UUID organizationId);

    List<AuditLogEntity> findAllByOrderByOccurredAtDesc();
}
