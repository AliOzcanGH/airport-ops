package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrganizationRepository
        extends JpaRepository<OrganizationEntity, UUID> {

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM iam.organizations organization_record
                WHERE lower(organization_record.name) = lower(:name)
                  AND organization_record.deleted_at IS NULL
            )
            """, nativeQuery = true)
    boolean existsNonDeletedByName(@Param("name") String name);
}
