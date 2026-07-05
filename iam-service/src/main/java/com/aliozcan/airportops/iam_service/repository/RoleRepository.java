package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.RoleEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.RoleScope;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends Repository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCodeAndScope(String code, RoleScope scope);
}
