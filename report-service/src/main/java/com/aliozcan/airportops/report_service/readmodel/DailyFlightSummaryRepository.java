package com.aliozcan.airportops.report_service.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DailyFlightSummaryRepository extends JpaRepository<DailyFlightSummaryEntity, UUID> {

    Optional<DailyFlightSummaryEntity> findByOrganizationIdAndSummaryDate(
            UUID organizationId, LocalDate summaryDate);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            INSERT INTO report.daily_flight_summary
                (organization_id, summary_date, total_flights, delayed_flights, cancelled_flights)
            VALUES (:organizationId, :summaryDate, 1, 0, 0)
            ON CONFLICT (organization_id, summary_date)
            DO UPDATE SET total_flights = report.daily_flight_summary.total_flights + 1,
                           updated_at = now()
            """)
    void incrementTotalFlights(
            @Param("organizationId") UUID organizationId, @Param("summaryDate") LocalDate summaryDate);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            INSERT INTO report.daily_flight_summary
                (organization_id, summary_date, total_flights, delayed_flights, cancelled_flights)
            VALUES (:organizationId, :summaryDate, 0, 1, 0)
            ON CONFLICT (organization_id, summary_date)
            DO UPDATE SET delayed_flights = report.daily_flight_summary.delayed_flights + 1,
                           updated_at = now()
            """)
    void incrementDelayedFlights(
            @Param("organizationId") UUID organizationId, @Param("summaryDate") LocalDate summaryDate);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            INSERT INTO report.daily_flight_summary
                (organization_id, summary_date, total_flights, delayed_flights, cancelled_flights)
            VALUES (:organizationId, :summaryDate, 0, 0, 1)
            ON CONFLICT (organization_id, summary_date)
            DO UPDATE SET cancelled_flights = report.daily_flight_summary.cancelled_flights + 1,
                           updated_at = now()
            """)
    void incrementCancelledFlights(
            @Param("organizationId") UUID organizationId, @Param("summaryDate") LocalDate summaryDate);
}
