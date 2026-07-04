package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.domain.model.InvitationEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationStatus;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.InvitationValidationResponse;
import com.aliozcan.airportops.iam_service.repository.InvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class InvitationValidationService {

    private final InvitationRepository invitationRepository;
    private final InvitationTokenService invitationTokenService;
    private final InvitationEmailMasker invitationEmailMasker;

    public InvitationValidationService(
            InvitationRepository invitationRepository,
            InvitationTokenService invitationTokenService,
            InvitationEmailMasker invitationEmailMasker) {
        this.invitationRepository = invitationRepository;
        this.invitationTokenService = invitationTokenService;
        this.invitationEmailMasker = invitationEmailMasker;
    }

    @Transactional(readOnly = true)
    public InvitationValidationResponse validate(String rawToken) {
        String tokenHash = invitationTokenService.hash(rawToken);
        InvitationEntity invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvitationNotFoundException::new);

        Instant now = Instant.now();
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

        return new InvitationValidationResponse(
                invitation.getCompanyName(),
                invitationEmailMasker.mask(invitation.getAdminEmail()),
                invitation.getExpiresAt()
        );
    }
}
