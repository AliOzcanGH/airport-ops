package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.MemberRoleEntity;
import com.aliozcan.airportops.iam_service.domain.model.MemberRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MemberRoleRepository
        extends JpaRepository<MemberRoleEntity, MemberRoleId> {

    @Query(value = """
            SELECT r.code
            FROM iam.member_roles mr
            JOIN iam.roles r ON r.id = mr.role_id
            WHERE mr.member_id = :memberId
            """, nativeQuery = true)
    List<String> findRoleCodesByMemberId(@Param("memberId") UUID memberId);

    long deleteByIdMemberId(UUID memberId);
}
