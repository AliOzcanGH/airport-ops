package com.aliozcan.airportops.report_service.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flight_report_entries")
public class FlightReportEntryEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "flight_id")
    private UUID flightId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FlightReportEntryEntity() {
    }

    public FlightReportEntryEntity(
            UUID flightId, UUID organizationId, String eventType, Instant occurredAt) {
        this.flightId = flightId;
        this.organizationId = organizationId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getFlightId() {
        return flightId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
