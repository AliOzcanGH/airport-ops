package com.aliozcan.airportops.iam_service.domain.model;

import com.aliozcan.airportops.iam_service.domain.model.enums.AuthProvider;
import com.aliozcan.airportops.iam_service.domain.model.enums.PreferredLanguage;
import com.aliozcan.airportops.iam_service.domain.model.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "iam")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 30)
    private AuthProvider authProvider;

    @Column(name = "keycloak_user_id", length = 64)
    private String keycloakUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", nullable = false, length = 2)
    private PreferredLanguage preferredLanguage;

    protected UserEntity() {
    }

    private UserEntity(
            UUID id,
            String email,
            String passwordHash,
            String fullName,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            AuthProvider authProvider,
            String keycloakUserId,
            PreferredLanguage preferredLanguage) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.authProvider = authProvider;
        this.keycloakUserId = keycloakUserId;
        this.preferredLanguage = preferredLanguage;
    }

    public static UserEntity provisioningKeycloakUser(
            String email,
            String fullName,
            PreferredLanguage preferredLanguage,
            Instant now) {
        return new UserEntity(
                UUID.randomUUID(),
                email,
                null,
                fullName,
                UserStatus.PROVISIONING,
                now,
                now,
                null,
                AuthProvider.KEYCLOAK,
                null,
                preferredLanguage
        );
    }

    public void activateWithKeycloakSubject(String keycloakUserId, Instant now) {
        if (status != UserStatus.PROVISIONING) {
            throw new IllegalStateException("User is not awaiting Keycloak provisioning");
        }
        this.keycloakUserId = keycloakUserId;
        this.status = UserStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void markKeycloakSyncFailed(Instant now) {
        if (status != UserStatus.PROVISIONING) {
            throw new IllegalStateException("User is not awaiting Keycloak provisioning");
        }
        this.keycloakUserId = null;
        this.status = UserStatus.KEYCLOAK_SYNC_FAILED;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public PreferredLanguage getPreferredLanguage() {
        return preferredLanguage;
    }
}
