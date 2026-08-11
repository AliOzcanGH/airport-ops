package com.aliozcan.airportops.report_service.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationOperationalSummaryRepository
        extends JpaRepository<OrganizationOperationalSummaryEntity, UUID> {

    Optional<OrganizationOperationalSummaryEntity> findByOrganizationId(UUID organizationId);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            INSERT INTO report.organization_operational_summary
                (organization_id, station_count, total_flights_last_30_days)
            VALUES (:organizationId, 1, 0)
            ON CONFLICT (organization_id)
            DO UPDATE SET station_count = report.organization_operational_summary.station_count + 1,
                           updated_at = now()
            """)
    void incrementStationCount(@Param("organizationId") UUID organizationId);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            INSERT INTO report.organization_operational_summary
                (organization_id, station_count, total_flights_last_30_days, last_flight_activity_at)
            VALUES (:organizationId, 0, 1, :occurredAt)
            ON CONFLICT (organization_id)
            DO UPDATE SET total_flights_last_30_days =
                               report.organization_operational_summary.total_flights_last_30_days + 1,
                           last_flight_activity_at = :occurredAt,
                           updated_at = now()
            """)
    void incrementFlightActivity(
            @Param("organizationId") UUID organizationId, @Param("occurredAt") Instant occurredAt);
}
