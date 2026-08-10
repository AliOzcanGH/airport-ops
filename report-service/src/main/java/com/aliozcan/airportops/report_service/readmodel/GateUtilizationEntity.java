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
@Table(name = "gate_utilization")
public class GateUtilizationEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "gate_id", nullable = false)
    private UUID gateId;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "flight_count", nullable = false)
    private int flightCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GateUtilizationEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getGateId() {
        return gateId;
    }

    public LocalDate getSummaryDate() {
        return summaryDate;
    }

    public int getFlightCount() {
        return flightCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
