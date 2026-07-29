package com.aliozcan.airportops.airport_service.station;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stations")
public class StationEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "station_name", nullable = false)
    private String stationName;

    @Column(name = "airport_code", nullable = false)
    private String airportCode;

    @Column(name = "gate_count", nullable = false)
    private int gateCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StationEntity() {
    }

    public StationEntity(UUID organizationId, String stationName, String airportCode, int gateCount) {
        this.organizationId = organizationId;
        this.stationName = stationName;
        this.airportCode = airportCode;
        this.gateCount = gateCount;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getStationName() {
        return stationName;
    }

    public String getAirportCode() {
        return airportCode;
    }

    public int getGateCount() {
        return gateCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
