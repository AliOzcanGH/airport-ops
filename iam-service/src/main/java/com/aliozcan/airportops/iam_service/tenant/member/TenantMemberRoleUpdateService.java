package com.aliozcan.airportops.iam_service.tenant.member;

import com.aliozcan.airportops.iam_service.domain.model.MemberRoleEntity;
import com.aliozcan.airportops.iam_service.domain.model.OrganizationMemberEntity;
import com.aliozcan.airportops.iam_service.domain.model.RoleEntity;
import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.RoleScope;
import com.aliozcan.airportops.iam_service.repository.MemberRoleRepository;
import com.aliozcan.airportops.iam_service.repository.OrganizationMemberRepository;
import com.aliozcan.airportops.iam_service.repository.RoleRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import com.aliozcan.airportops.iam_service.tenant.TenantContext;
import com.aliozcan.airportops.iam_service.tenant.member.dto.MemberRoleUpdateResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TenantMemberRoleUpdateService {

    private final TenantMemberAccessGuard accessGuard;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TenantMemberRoleUpdateService(
            TenantMemberAccessGuard accessGuard,
            OrganizationMemberRepository organizationMemberRepository,
            MemberRoleRepository memberRoleRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {
        this.accessGuard = accessGuard;
        this.organizationMemberRepository = organizationMemberRepository;
        this.memberRoleRepository = memberRoleRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public MemberRoleUpdateResponse update(
            UUID pathOrganizationId, UUID memberId, String newRoleCode, UUID callerUserId) {
        TenantContext context = accessGuard.requireOrganizationAdmin(callerUserId, pathOrganizationId);

        OrganizationMemberEntity member = organizationMemberRepository
                .findByIdAndOrganizationId(memberId, pathOrganizationId)
                .orElseThrow(MemberNotFoundException::new);

        organizationMemberRepository.findByOrganizationIdAndUserId(pathOrganizationId, callerUserId)
                .filter(callerMember -> callerMember.getId().equals(memberId))
                .ifPresent(callerMember -> {
                    throw new CannotModifyOwnRoleException();
                });

        List<String> previousRoles = memberRoleRepository.findRoleCodesByMemberId(memberId);
        String previousRole = previousRoles.isEmpty() ? null : previousRoles.get(0);

        RoleEntity newRole = roleRepository.findByCodeAndScope(newRoleCode, RoleScope.ORGANIZATION)
                .orElseThrow(() -> new IllegalStateException("Unknown organization role: " + newRoleCode));

        memberRoleRepository.deleteByIdMemberId(memberId);
        memberRoleRepository.save(MemberRoleEntity.assign(member.getId(), newRole.getId()));

        String actorEmail = userRepository.findActiveById(callerUserId)
                .map(UserEntity::getEmail)
                .orElse(null);

        eventPublisher.publishEvent(new MemberRoleUpdatedEvent(
                pathOrganizationId,
                callerUserId,
                actorEmail,
                memberId,
                previousRole,
                newRoleCode,
                Instant.now()));

        return new MemberRoleUpdateResponse(memberId, newRoleCode);
    }
}
