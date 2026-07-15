package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.domain.model.InvitationEntity;
import com.aliozcan.airportops.iam_service.repository.InvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class InvitationEmailDeliveryStateService {

    private final InvitationRepository invitationRepository;

    public InvitationEmailDeliveryStateService(
            InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    @Transactional
    public InvitationEntity markSent(UUID invitationId, Instant now) {
        InvitationEntity invitation = load(invitationId);
        invitation.markEmailSent(now);
        return invitation;
    }

    @Transactional
    public InvitationEntity markFailed(
            UUID invitationId,
            String sanitizedReason,
            Instant now) {
        InvitationEntity invitation = load(invitationId);
        invitation.markEmailFailed(sanitizedReason, now);
        return invitation;
    }

    private InvitationEntity load(UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Invitation not found while updating email delivery state"));
    }
}
