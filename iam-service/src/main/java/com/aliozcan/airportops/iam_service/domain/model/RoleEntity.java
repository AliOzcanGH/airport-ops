package com.aliozcan.airportops.iam_service.domain.model;

import com.aliozcan.airportops.iam_service.domain.model.enums.RoleScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "roles", schema = "iam")
public class RoleEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoleScope scope;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RoleEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public RoleScope getScope() {
        return scope;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
