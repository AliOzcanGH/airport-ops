package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.domain.model.InvitationEntity;
import com.aliozcan.airportops.iam_service.platform.invitation.InvitationTokenService.GeneratedInvitationToken;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.CreatePlatformInvitationRequest;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.PlatformInvitationResponse;
import com.aliozcan.airportops.iam_service.repository.InvitationRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class PlatformInvitationService {

    private static final Duration INVITATION_TTL = Duration.ofHours(72);
    private static final String PENDING_EMAIL_CONSTRAINT =
            "uq_invitations_pending_admin_email";

    private final InvitationRepository invitationRepository;
    private final InvitationTokenService invitationTokenService;

    public PlatformInvitationService(
            InvitationRepository invitationRepository,
            InvitationTokenService invitationTokenService) {
        this.invitationRepository = invitationRepository;
        this.invitationTokenService = invitationTokenService;
    }

    @Transactional
    public PlatformInvitationResponse create(
            CreatePlatformInvitationRequest request,
            UUID createdByUserId) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String organizationName = request.organizationName().trim();

        if (invitationRepository.existsPendingByAdminEmail(email)) {
            throw new PendingInvitationExistsException();
        }

        Instant createdAt = Instant.now();
        GeneratedInvitationToken token = invitationTokenService.generate();
        InvitationEntity invitation = InvitationEntity.pending(
                organizationName,
                email,
                token.tokenHash(),
                createdByUserId,
                createdAt,
                createdAt.plus(INVITATION_TTL)
        );

        InvitationEntity savedInvitation;
        try {
            savedInvitation = invitationRepository.saveAndFlush(invitation);
        } catch (DataIntegrityViolationException exception) {
            if (violatesPendingEmailConstraint(exception)) {
                throw new PendingInvitationExistsException();
            }
            throw exception;
        }

        return new PlatformInvitationResponse(
                savedInvitation.getId(),
                savedInvitation.getAdminEmail(),
                savedInvitation.getCompanyName(),
                savedInvitation.getStatus(),
                savedInvitation.getExpiresAt(),
                token.rawToken()
        );
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
