package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.repository.projection.PlatformAuthorizationRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PlatformAuthorizationRepository extends Repository<UserEntity, UUID> {

    @Query(value = """
            SELECT r.code AS "roleCode",
                   p.code AS "permissionCode"
            FROM iam.users u
            JOIN iam.platform_user_roles pur
              ON pur.user_id = u.id
            JOIN iam.roles r
              ON r.id = pur.role_id
             AND r.scope = 'PLATFORM'
            LEFT JOIN iam.role_permissions rp
              ON rp.role_id = r.id
            LEFT JOIN iam.permissions p
              ON p.id = rp.permission_id
             AND p.scope = r.scope
            WHERE u.id = :userId
              AND u.status = 'ACTIVE'
              AND u.deleted_at IS NULL
            ORDER BY r.code, p.code
            """, nativeQuery = true)
    List<PlatformAuthorizationRow> findPlatformAuthorizationByUserId(
            @Param("userId") UUID userId);
}
