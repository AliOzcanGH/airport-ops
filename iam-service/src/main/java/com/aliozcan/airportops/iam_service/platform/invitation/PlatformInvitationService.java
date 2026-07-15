package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.domain.model.InvitationEntity;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.CreatePlatformInvitationRequest;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.PlatformInvitationResponse;
import com.aliozcan.airportops.iam_service.platform.invitation.email.InvitationAcceptUrlBuilder;
import com.aliozcan.airportops.iam_service.platform.invitation.email.InvitationEmailFailureSanitizer;
import com.aliozcan.airportops.iam_service.platform.invitation.email.InvitationEmailMessage;
import com.aliozcan.airportops.iam_service.platform.invitation.email.InvitationEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PlatformInvitationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PlatformInvitationService.class);

    private final PlatformInvitationCreationTransactionService creationService;
    private final InvitationEmailDeliveryStateService deliveryStateService;
    private final InvitationEmailSender emailSender;
    private final InvitationAcceptUrlBuilder acceptUrlBuilder;
    private final InvitationEmailFailureSanitizer failureSanitizer;

    public PlatformInvitationService(
            PlatformInvitationCreationTransactionService creationService,
            InvitationEmailDeliveryStateService deliveryStateService,
            InvitationEmailSender emailSender,
            InvitationAcceptUrlBuilder acceptUrlBuilder,
            InvitationEmailFailureSanitizer failureSanitizer) {
        this.creationService = creationService;
        this.deliveryStateService = deliveryStateService;
        this.emailSender = emailSender;
        this.acceptUrlBuilder = acceptUrlBuilder;
        this.failureSanitizer = failureSanitizer;
    }

    public PlatformInvitationResponse create(
            CreatePlatformInvitationRequest request,
            UUID createdByUserId) {
        CreatedPlatformInvitation created = creationService.create(
                request,
                createdByUserId);
        InvitationEntity invitation = created.invitation();
        String acceptUrl = acceptUrlBuilder.build(created.rawToken());

        InvitationEntity invitationWithDeliveryState = sendEmailAndRecordState(
                invitation,
                acceptUrl);

        return response(invitationWithDeliveryState, acceptUrl);
    }

    private InvitationEntity sendEmailAndRecordState(
            InvitationEntity invitation,
            String acceptUrl) {
        try {
            emailSender.send(new InvitationEmailMessage(
                    invitation.getAdminEmail(),
                    invitation.getCompanyName(),
                    acceptUrl,
                    invitation.getExpiresAt()));
            try {
                return deliveryStateService.markSent(
                        invitation.getId(),
                        Instant.now());
            } catch (RuntimeException updateFailure) {
                LOGGER.error(
                        "Invitation email sent but delivery state update failed. invitationId={}, exceptionType={}, message={}",
                        invitation.getId(),
                        updateFailure.getClass().getName(),
                        updateFailure.getMessage());
                return invitation;
            }
        } catch (RuntimeException deliveryFailure) {
            String sanitizedReason = failureSanitizer.sanitize(deliveryFailure);
            try {
                return deliveryStateService.markFailed(
                        invitation.getId(),
                        sanitizedReason,
                        Instant.now());
            } catch (RuntimeException updateFailure) {
                LOGGER.error(
                        "Invitation email failed and delivery state update failed. invitationId={}, deliveryExceptionType={}, updateExceptionType={}, updateMessage={}",
                        invitation.getId(),
                        deliveryFailure.getClass().getName(),
                        updateFailure.getClass().getName(),
                        updateFailure.getMessage());
                return invitation;
            }
        }
    }

    private PlatformInvitationResponse response(
            InvitationEntity invitation,
            String acceptUrl) {
        return new PlatformInvitationResponse(
                invitation.getId(),
                invitation.getAdminEmail(),
                invitation.getCompanyName(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getEmailDeliveryStatus(),
                invitation.getEmailSentAt(),
                acceptUrlBuilder.devLinkEnabled() ? acceptUrl : null
        );
    }
}
