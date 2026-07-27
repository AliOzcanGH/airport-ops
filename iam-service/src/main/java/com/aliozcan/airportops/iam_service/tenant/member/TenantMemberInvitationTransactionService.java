package com.aliozcan.airportops.iam_service.tenant.member;

import com.aliozcan.airportops.iam_service.domain.model.InvitationEntity;
import com.aliozcan.airportops.iam_service.platform.invitation.InvitationTokenService;
import com.aliozcan.airportops.iam_service.platform.invitation.InvitationTokenService.GeneratedInvitationToken;
import com.aliozcan.airportops.iam_service.platform.invitation.PendingInvitationExistsException;
import com.aliozcan.airportops.iam_service.repository.InvitationRepository;
import com.aliozcan.airportops.iam_service.repository.OrganizationMemberRepository;
import com.aliozcan.airportops.iam_service.tenant.member.dto.InviteOrganizationMemberRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class TenantMemberInvitationTransactionService {

    private static final Duration INVITATION_TTL = Duration.ofHours(72);
    private static final String PENDING_EMAIL_CONSTRAINT =
            "uq_invitations_pending_admin_email";

    private final InvitationRepository invitationRepository;
    private final InvitationTokenService invitationTokenService;
    private final OrganizationMemberRepository organizationMemberRepository;

    public TenantMemberInvitationTransactionService(
            InvitationRepository invitationRepository,
            InvitationTokenService invitationTokenService,
            OrganizationMemberRepository organizationMemberRepository) {
        this.invitationRepository = invitationRepository;
        this.invitationTokenService = invitationTokenService;
        this.organizationMemberRepository = organizationMemberRepository;
    }

    @Transactional
    public CreatedOrganizationMemberInvitation create(
            UUID organizationId,
            String organizationName,
            InviteOrganizationMemberRequest request,
            UUID createdByUserId) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (organizationMemberRepository.existsActiveMemberByOrganizationIdAndEmail(
                organizationId, email)) {
            throw new OrganizationMemberAlreadyExistsException();
        }
        if (invitationRepository.existsPendingByAdminEmail(email)) {
            throw new PendingInvitationExistsException();
        }

        Instant createdAt = Instant.now();
        GeneratedInvitationToken token = invitationTokenService.generate();
        InvitationEntity invitation = InvitationEntity.pendingForOrganization(
                organizationId,
                organizationName,
                email,
                request.fullName(),
                request.intendedRole(),
                token.tokenHash(),
                createdByUserId,
                createdAt,
                createdAt.plus(INVITATION_TTL)
        );

        try {
            return new CreatedOrganizationMemberInvitation(
                    invitationRepository.saveAndFlush(invitation),
                    token.rawToken());
        } catch (DataIntegrityViolationException exception) {
            if (violatesPendingEmailConstraint(exception)) {
                throw new PendingInvitationExistsException();
            }
            throw exception;
        }
    }

    private boolean violatesPendingEmailConstraint(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && PENDING_EMAIL_CONSTRAINT.equals(
                            constraintViolation.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
