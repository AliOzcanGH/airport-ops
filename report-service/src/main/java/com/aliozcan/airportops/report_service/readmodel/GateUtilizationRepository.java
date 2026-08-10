package com.aliozcan.airportops.report_service.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GateUtilizationRepository extends JpaRepository<GateUtilizationEntity, UUID> {

    List<GateUtilizationEntity> findByOrganizationIdAndSummaryDate(UUID organizationId, LocalDate summaryDate);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            INSERT INTO report.gate_utilization
                (organization_id, gate_id, summary_date, flight_count)
            VALUES (:organizationId, :gateId, :summaryDate, 1)
            ON CONFLICT (organization_id, gate_id, summary_date)
            DO UPDATE SET flight_count = report.gate_utilization.flight_count + 1,
                           updated_at = now()
            """)
    void incrementFlightCount(
            @Param("organizationId") UUID organizationId,
            @Param("gateId") UUID gateId,
            @Param("summaryDate") LocalDate summaryDate);
}
