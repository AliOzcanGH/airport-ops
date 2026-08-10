package com.aliozcan.airportops.report_service.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_flight_summary")
public class DailyFlightSummaryEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "total_flights", nullable = false)
    private int totalFlights;

    @Column(name = "delayed_flights", nullable = false)
    private int delayedFlights;

    @Column(name = "cancelled_flights", nullable = false)
    private int cancelledFlights;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DailyFlightSummaryEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public LocalDate getSummaryDate() {
        return summaryDate;
    }

    public int getTotalFlights() {
        return totalFlights;
    }

    public int getDelayedFlights() {
        return delayedFlights;
    }

    public int getCancelledFlights() {
        return cancelledFlights;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
