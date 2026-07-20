package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.OrganizationMemberEntity;
import com.aliozcan.airportops.iam_service.repository.projection.TenantAuthorizationRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TenantAuthorizationRepository
        extends Repository<OrganizationMemberEntity, UUID> {

    @Query(value = """
            SELECT member.organization_id AS "organizationId",
                   organization_record.name AS "organizationName",
                   organization_record.status AS "organizationStatus",
                   role_record.code AS "roleCode",
                   permission_record.code AS "permissionCode"
            FROM iam.organization_members member
            JOIN iam.organizations organization_record
              ON organization_record.id = member.organization_id
             AND organization_record.deleted_at IS NULL
             AND organization_record.status IN ('ONBOARDING_INCOMPLETE', 'ACTIVE')
            LEFT JOIN iam.member_roles member_role
              ON member_role.member_id = member.id
            LEFT JOIN iam.roles role_record
              ON role_record.id = member_role.role_id
             AND role_record.scope = 'ORGANIZATION'
            LEFT JOIN iam.role_permissions role_permission
              ON role_permission.role_id = role_record.id
            LEFT JOIN iam.permissions permission_record
              ON permission_record.id = role_permission.permission_id
             AND permission_record.scope = role_record.scope
            WHERE member.user_id = :userId
              AND member.status = 'ACTIVE'
              AND member.deleted_at IS NULL
            ORDER BY role_record.code, permission_record.code
            """, nativeQuery = true)
    List<TenantAuthorizationRow> findTenantAuthorizationByUserId(
            @Param("userId") UUID userId);

    @Query(value = """
            SELECT organization_record.id
            FROM iam.organizations organization_record
            JOIN iam.organization_members member
              ON member.organization_id = organization_record.id
             AND member.user_id = :userId
             AND member.status = 'ACTIVE'
             AND member.deleted_at IS NULL
            WHERE organization_record.status = 'ONBOARDING_INCOMPLETE'
              AND organization_record.deleted_at IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM iam.member_roles member_role
                  JOIN iam.roles role_record
                    ON role_record.id = member_role.role_id
                   AND role_record.scope = 'ORGANIZATION'
                   AND role_record.code = 'AIRLINE_ADMIN'
                  WHERE member_role.member_id = member.id
              )
            FOR UPDATE OF organization_record
            """, nativeQuery = true)
    List<UUID> findOnboardingAirlineAdminOrganizationIdsForUpdate(
            @Param("userId") UUID userId);

    @Query(value = """
            SELECT organization_record.id
            FROM iam.organizations organization_record
            JOIN iam.organization_members member
              ON member.organization_id = organization_record.id
             AND member.user_id = :userId
             AND member.status = 'ACTIVE'
             AND member.deleted_at IS NULL
            WHERE organization_record.status = 'ACTIVE'
              AND organization_record.deleted_at IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM iam.member_roles member_role
                  JOIN iam.roles role_record
                    ON role_record.id = member_role.role_id
                   AND role_record.scope = 'ORGANIZATION'
                   AND role_record.code = 'AIRLINE_ADMIN'
                  WHERE member_role.member_id = member.id
              )
            ORDER BY organization_record.id
            FOR UPDATE OF organization_record
            """, nativeQuery = true)
    List<UUID> findActiveAirlineAdminOrganizationIdsForCompletionForUpdate(
            @Param("userId") UUID userId);
}
