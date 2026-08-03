package com.aliozcan.airportops.airport_service.gate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gates")
public class GateEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "terminal")
    private String terminal;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GateEntity() {
    }

    public GateEntity(UUID stationId, String code, String terminal, String status) {
        this.stationId = stationId;
        this.code = code;
        this.terminal = terminal;
        this.status = status;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStationId() {
        return stationId;
    }

    public String getCode() {
        return code;
    }

    public String getTerminal() {
        return terminal;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
}
