package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.UserTotpCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserTotpCredentialRepository
        extends JpaRepository<UserTotpCredentialEntity, UUID> {

    Optional<UserTotpCredentialEntity> findByUserId(UUID userId);

    @Query(value = """
            SELECT credential.*
            FROM iam.user_totp_credentials credential
            WHERE credential.user_id = :userId
              AND credential.status = 'ENABLED'
            """, nativeQuery = true)
    Optional<UserTotpCredentialEntity> findEnabledByUserId(
            @Param("userId") UUID userId);
}
