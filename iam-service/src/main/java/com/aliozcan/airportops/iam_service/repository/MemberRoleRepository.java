package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.MemberRoleEntity;
import com.aliozcan.airportops.iam_service.domain.model.MemberRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRoleRepository
        extends JpaRepository<MemberRoleEntity, MemberRoleId> {
}
