package com.aliozcan.airportops.report_service.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_operational_summary")
public class OrganizationOperationalSummaryEntity {

    @Id
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "station_count", nullable = false)
    private int stationCount;

    @Column(name = "total_flights_last_30_days", nullable = false)
    private int totalFlightsLast30Days;

    @Column(name = "last_flight_activity_at")
    private Instant lastFlightActivityAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationOperationalSummaryEntity() {
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public int getStationCount() {
        return stationCount;
    }

    public int getTotalFlightsLast30Days() {
        return totalFlightsLast30Days;
    }

    public Instant getLastFlightActivityAt() {
        return lastFlightActivityAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
