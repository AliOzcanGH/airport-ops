package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.MfaLoginChallengeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MfaLoginChallengeRepository
        extends JpaRepository<MfaLoginChallengeEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT challenge FROM MfaLoginChallengeEntity challenge "
            + "WHERE challenge.id = :challengeId")
    Optional<MfaLoginChallengeEntity> findByIdForUpdate(
            @Param("challengeId") UUID challengeId);
}
