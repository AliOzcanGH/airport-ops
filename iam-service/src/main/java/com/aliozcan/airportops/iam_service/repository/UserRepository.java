package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT user FROM UserEntity user WHERE user.id = :userId")
    Optional<UserEntity> findByIdForUpdate(@Param("userId") UUID userId);

    @Query(value = """
            SELECT u.*
            FROM iam.users u
            WHERE lower(u.email) = lower(:email)
              AND u.status = 'ACTIVE'
              AND u.deleted_at IS NULL
            """, nativeQuery = true)
    Optional<UserEntity> findActiveByEmail(@Param("email") String email);

    @Query(value = """
            SELECT u.*
            FROM iam.users u
            WHERE u.id = :userId
              AND u.status = 'ACTIVE'
              AND u.deleted_at IS NULL
            """, nativeQuery = true)
    Optional<UserEntity> findActiveById(@Param("userId") UUID userId);

    @Query(value = """
            SELECT u.*
            FROM iam.users u
            WHERE lower(u.email) = lower(:email)
              AND u.status = 'ACTIVE'
              AND u.auth_provider = 'LOCAL'
              AND u.deleted_at IS NULL
            """, nativeQuery = true)
    Optional<UserEntity> findActiveLocalByEmail(@Param("email") String email);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM iam.users u
                WHERE lower(u.email) = lower(:email)
                  AND u.deleted_at IS NULL
            )
            """, nativeQuery = true)
    boolean existsNonDeletedByEmail(@Param("email") String email);
}
