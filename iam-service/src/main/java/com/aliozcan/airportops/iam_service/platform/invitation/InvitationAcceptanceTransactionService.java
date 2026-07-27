package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.domain.model.InvitationEntity;
import com.aliozcan.airportops.iam_service.domain.model.MemberRoleEntity;
import com.aliozcan.airportops.iam_service.domain.model.OrganizationEntity;
import com.aliozcan.airportops.iam_service.domain.model.OrganizationMemberEntity;
import com.aliozcan.airportops.iam_service.domain.model.RoleEntity;
import com.aliozcan.airportops.iam_service.domain.model.UserEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationStatus;
import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationType;
import com.aliozcan.airportops.iam_service.domain.model.enums.PreferredLanguage;
import com.aliozcan.airportops.iam_service.domain.model.enums.RoleScope;
import com.aliozcan.airportops.iam_service.repository.InvitationRepository;
import com.aliozcan.airportops.iam_service.repository.MemberRoleRepository;
import com.aliozcan.airportops.iam_service.repository.OrganizationMemberRepository;
import com.aliozcan.airportops.iam_service.repository.OrganizationRepository;
import com.aliozcan.airportops.iam_service.repository.RoleRepository;
import com.aliozcan.airportops.iam_service.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class InvitationAcceptanceTransactionService {

    private static final String AIRLINE_ADMIN_ROLE = "AIRLINE_ADMIN";
    private static final String USER_EMAIL_CONSTRAINT = "uq_users_active_email";
    private static final String ORGANIZATION_NAME_CONSTRAINT =
            "uq_organizations_active_name";

    private final InvitationRepository invitationRepository;
    private final InvitationTokenService invitationTokenService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final RoleRepository roleRepository;
    private final MemberRoleRepository memberRoleRepository;

    public InvitationAcceptanceTransactionService(
            InvitationRepository invitationRepository,
            InvitationTokenService invitationTokenService,
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            RoleRepository roleRepository,
            MemberRoleRepository memberRoleRepository) {
        this.invitationRepository = invitationRepository;
        this.invitationTokenService = invitationTokenService;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.roleRepository = roleRepository;
        this.memberRoleRepository = memberRoleRepository;
    }

    @Transactional
    public IamProvisioningResult provision(
            String rawToken,
            String fullName,
            String preferredLanguage) {
        String tokenHash = invitationTokenService.hash(rawToken);
        InvitationEntity invitation = invitationRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(InvitationNotFoundException::new);

        Instant now = Instant.now();
        validateInvitation(invitation, now);

        if (userRepository.existsNonDeletedByEmail(invitation.getAdminEmail())) {
            throw new IamUserAlreadyExistsException();
        }

        if (invitation.getInvitationType() == InvitationType.ORGANIZATION) {
            return provisionOrganizationMember(invitation, fullName, preferredLanguage, now);
        }
        return provisionPlatformTenant(invitation, fullName, preferredLanguage, now);
    }

    private IamProvisioningResult provisionPlatformTenant(
            InvitationEntity invitation,
            String fullName,
            String preferredLanguage,
            Instant now) {
        if (organizationRepository.existsNonDeletedByName(
                invitation.getCompanyName())) {
            throw new OrganizationAlreadyExistsException();
        }

        RoleEntity airlineAdminRole = roleRepository.findByCodeAndScope(
                        AIRLINE_ADMIN_ROLE,
                        RoleScope.ORGANIZATION)
                .orElseThrow(() -> new ProvisioningInvariantException(
                        "Canonical AIRLINE_ADMIN organization role is missing"));

        try {
            UserEntity user = UserEntity.provisioningKeycloakUser(
                    invitation.getAdminEmail(),
                    fullName,
                    PreferredLanguage.valueOf(preferredLanguage),
                    now);
            userRepository.saveAndFlush(user);

            OrganizationEntity organization = OrganizationEntity.onboarding(
                    invitation.getCompanyName(),
                    now);
            organizationRepository.saveAndFlush(organization);

            OrganizationMemberEntity member = OrganizationMemberEntity.active(
                    organization.getId(),
                    user.getId(),
                    now);
            organizationMemberRepository.saveAndFlush(member);

            MemberRoleEntity memberRole = MemberRoleEntity.assign(
                    member.getId(),
                    airlineAdminRole.getId());
            memberRoleRepository.saveAndFlush(memberRole);

            invitation.accept(organization.getId(), now);
            invitationRepository.saveAndFlush(invitation);

            return new IamProvisioningResult(
                    user.getId(),
                    user.getEmail(),
                    organization.getName(),
                    organization.getStatus()
            );
        } catch (DataIntegrityViolationException exception) {
            if (violatesConstraint(exception, USER_EMAIL_CONSTRAINT)) {
                throw new IamUserAlreadyExistsException();
            }
            if (violatesConstraint(exception, ORGANIZATION_NAME_CONSTRAINT)) {
                throw new OrganizationAlreadyExistsException();
            }
            throw exception;
        }
    }

    private IamProvisioningResult provisionOrganizationMember(
            InvitationEntity invitation,
            String fullName,
            String preferredLanguage,
            Instant now) {
        OrganizationEntity organization = organizationRepository
                .findById(invitation.getOrganizationId())
                .orElseThrow(() -> new ProvisioningInvariantException(
                        "Organization referenced by invitation no longer exists"));

        RoleEntity intendedRole = roleRepository.findByCodeAndScope(
                        invitation.getIntendedRole(),
                        RoleScope.ORGANIZATION)
                .orElseThrow(() -> new ProvisioningInvariantException(
                        "Intended organization role is missing"));

        try {
            UserEntity user = UserEntity.provisioningKeycloakUser(
                    invitation.getAdminEmail(),
                    fullName,
                    PreferredLanguage.valueOf(preferredLanguage),
                    now);
            userRepository.saveAndFlush(user);

            OrganizationMemberEntity member = OrganizationMemberEntity.active(
                    organization.getId(),
                    user.getId(),
                    now);
            organizationMemberRepository.saveAndFlush(member);

            MemberRoleEntity memberRole = MemberRoleEntity.assign(
                    member.getId(),
                    intendedRole.getId());
            memberRoleRepository.saveAndFlush(memberRole);

            invitation.accept(organization.getId(), now);
            invitationRepository.saveAndFlush(invitation);

            return new IamProvisioningResult(
                    user.getId(),
                    user.getEmail(),
                    organization.getName(),
                    organization.getStatus()
            );
        } catch (DataIntegrityViolationException exception) {
            if (violatesConstraint(exception, USER_EMAIL_CONSTRAINT)) {
                throw new IamUserAlreadyExistsException();
            }
            throw exception;
        }
    }

    private void validateInvitation(
            InvitationEntity invitation,
            Instant now) {
        InvitationStatus status = invitation.getStatus();
        if (status == InvitationStatus.CANCELLED) {
            throw new InvitationNotFoundException();
        }
        if (status == InvitationStatus.ACCEPTED) {
            throw new InvitationAlreadyUsedException();
        }
        if (status == InvitationStatus.EXPIRED
                || invitation.getExpiresAt().isBefore(now)) {
            throw new InvitationExpiredException();
        }
    }

    private boolean violatesConstraint(
            Throwable throwable,
            String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && constraintName.equals(
                            constraintViolation.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
