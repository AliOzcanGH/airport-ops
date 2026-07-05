package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.OrganizationMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationMemberRepository
        extends JpaRepository<OrganizationMemberEntity, UUID> {
}
