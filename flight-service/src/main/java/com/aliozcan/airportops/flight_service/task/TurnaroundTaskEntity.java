package com.aliozcan.airportops.flight_service.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "turnaround_tasks")
public class TurnaroundTaskEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "flight_id", nullable = false)
    private UUID flightId;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TurnaroundTaskEntity() {
    }

    public TurnaroundTaskEntity(UUID flightId, String taskType, String status) {
        this.flightId = flightId;
        this.taskType = taskType;
        this.status = status;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFlightId() {
        return flightId;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getStatus() {
        return status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateStatus(String newStatus, String newAssignedTo) {
        this.status = newStatus;
        if (newAssignedTo != null) {
            this.assignedTo = newAssignedTo;
        }
        this.updatedAt = Instant.now();
    }
}
