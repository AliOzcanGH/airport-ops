package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.OrganizationEntity;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformTenantMemberRow;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformTenantSummaryRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformTenantDirectoryRepository
        extends Repository<OrganizationEntity, UUID> {

    @Query(value = """
            SELECT organization_record.id AS "organizationId",
                   organization_record.name AS "organizationName",
                   organization_record.status AS "organizationStatus",
                   organization_record.created_at AS "createdAt",
                   COUNT(DISTINCT member.id) AS "memberCount",
                   MIN(lower(CASE
                       WHEN role_record.code = 'AIRLINE_ADMIN'
                        AND user_record.email IS NOT NULL
                       THEN user_record.email
                       ELSE NULL
                   END)) AS "primaryAdminEmail"
            FROM iam.organizations organization_record
            LEFT JOIN iam.organization_members member
              ON member.organization_id = organization_record.id
             AND member.status = 'ACTIVE'
             AND member.deleted_at IS NULL
            LEFT JOIN iam.users user_record
              ON user_record.id = member.user_id
             AND user_record.deleted_at IS NULL
            LEFT JOIN iam.member_roles member_role
              ON member_role.member_id = member.id
            LEFT JOIN iam.roles role_record
              ON role_record.id = member_role.role_id
             AND role_record.scope = 'ORGANIZATION'
            WHERE organization_record.deleted_at IS NULL
              AND organization_record.status IN (
                  'ONBOARDING_INCOMPLETE',
                  'ACTIVE',
                  'INACTIVE'
              )
            GROUP BY organization_record.id,
                     organization_record.name,
                     organization_record.status,
                     organization_record.created_at
            ORDER BY organization_record.created_at DESC,
                     lower(organization_record.name) ASC
            """, nativeQuery = true)
    List<PlatformTenantSummaryRow> findTenantSummaries();

    @Query(value = """
            SELECT organization_record.id AS "organizationId",
                   organization_record.name AS "organizationName",
                   organization_record.status AS "organizationStatus",
                   organization_record.created_at AS "createdAt",
                   COUNT(DISTINCT member.id) AS "memberCount",
                   MIN(lower(CASE
                       WHEN role_record.code = 'AIRLINE_ADMIN'
                        AND user_record.email IS NOT NULL
                       THEN user_record.email
                       ELSE NULL
                   END)) AS "primaryAdminEmail"
            FROM iam.organizations organization_record
            LEFT JOIN iam.organization_members member
              ON member.organization_id = organization_record.id
             AND member.status = 'ACTIVE'
             AND member.deleted_at IS NULL
            LEFT JOIN iam.users user_record
              ON user_record.id = member.user_id
             AND user_record.deleted_at IS NULL
            LEFT JOIN iam.member_roles member_role
              ON member_role.member_id = member.id
            LEFT JOIN iam.roles role_record
              ON role_record.id = member_role.role_id
             AND role_record.scope = 'ORGANIZATION'
            WHERE organization_record.id = :organizationId
              AND organization_record.deleted_at IS NULL
              AND organization_record.status IN (
                  'ONBOARDING_INCOMPLETE',
                  'ACTIVE',
                  'INACTIVE'
              )
            GROUP BY organization_record.id,
                     organization_record.name,
                     organization_record.status,
                     organization_record.created_at
            """, nativeQuery = true)
    Optional<PlatformTenantSummaryRow> findTenantSummaryById(
            @Param("organizationId") UUID organizationId);

    @Query(value = """
            SELECT member.id AS "memberId",
                   user_record.id AS "userId",
                   user_record.email AS "email",
                   user_record.full_name AS "fullName",
                   member.status AS "memberStatus",
                   member.joined_at AS "joinedAt",
                   role_record.code AS "roleCode"
            FROM iam.organization_members member
            JOIN iam.users user_record
              ON user_record.id = member.user_id
             AND user_record.deleted_at IS NULL
            LEFT JOIN iam.member_roles member_role
              ON member_role.member_id = member.id
            LEFT JOIN iam.roles role_record
              ON role_record.id = member_role.role_id
             AND role_record.scope = 'ORGANIZATION'
            WHERE member.organization_id = :organizationId
              AND member.status = 'ACTIVE'
              AND member.deleted_at IS NULL
            ORDER BY lower(user_record.email) ASC,
                     role_record.code ASC
            """, nativeQuery = true)
    List<PlatformTenantMemberRow> findActiveMembersByOrganizationId(
            @Param("organizationId") UUID organizationId);
}
