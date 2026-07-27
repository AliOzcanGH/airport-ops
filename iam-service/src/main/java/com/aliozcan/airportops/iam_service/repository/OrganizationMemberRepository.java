package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.OrganizationMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrganizationMemberRepository
        extends JpaRepository<OrganizationMemberEntity, UUID> {

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM iam.organization_members member
                JOIN iam.users user_record
                  ON user_record.id = member.user_id
                 AND user_record.deleted_at IS NULL
                WHERE member.organization_id = :organizationId
                  AND member.status = 'ACTIVE'
                  AND member.deleted_at IS NULL
                  AND lower(user_record.email) = lower(:email)
            )
            """, nativeQuery = true)
    boolean existsActiveMemberByOrganizationIdAndEmail(
            @Param("organizationId") UUID organizationId,
            @Param("email") String email);
}
