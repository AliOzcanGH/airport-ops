package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.InvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InvitationRepository extends JpaRepository<InvitationEntity, UUID> {

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM iam.invitations invitation
                WHERE lower(invitation.admin_email) = lower(:email)
                  AND invitation.status = 'PENDING'
            )
            """, nativeQuery = true)
    boolean existsPendingByAdminEmail(@Param("email") String email);
}
