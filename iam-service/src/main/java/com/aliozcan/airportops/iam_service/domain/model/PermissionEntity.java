package com.aliozcan.airportops.iam_service.domain.model;

import com.aliozcan.airportops.iam_service.domain.model.enums.PermissionScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "permissions", schema = "iam")
public class PermissionEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PermissionScope scope;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PermissionEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public PermissionScope getScope() {
        return scope;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
