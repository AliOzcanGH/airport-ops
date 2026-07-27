package com.aliozcan.airportops.iam_service.tenant.member;

import com.aliozcan.airportops.iam_service.domain.model.InvitationEntity;
import com.aliozcan.airportops.iam_service.platform.invitation.InvitationEmailDeliveryStateService;
import com.aliozcan.airportops.iam_service.platform.invitation.email.InvitationAcceptUrlBuilder;
import com.aliozcan.airportops.iam_service.platform.invitation.email.InvitationEmailFailureSanitizer;
import com.aliozcan.airportops.iam_service.platform.invitation.email.InvitationEmailMessage;
import com.aliozcan.airportops.iam_service.platform.invitation.email.InvitationEmailSender;
import com.aliozcan.airportops.iam_service.tenant.TenantContext;
import com.aliozcan.airportops.iam_service.tenant.member.dto.InviteOrganizationMemberRequest;
import com.aliozcan.airportops.iam_service.tenant.member.dto.OrganizationMemberInvitationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TenantMemberInvitationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TenantMemberInvitationService.class);

    private final TenantMemberAccessGuard accessGuard;
    private final TenantMemberInvitationTransactionService creationService;
    private final InvitationEmailDeliveryStateService deliveryStateService;
    private final InvitationEmailSender emailSender;
    private final InvitationAcceptUrlBuilder acceptUrlBuilder;
    private final InvitationEmailFailureSanitizer failureSanitizer;

    public TenantMemberInvitationService(
            TenantMemberAccessGuard accessGuard,
            TenantMemberInvitationTransactionService creationService,
            InvitationEmailDeliveryStateService deliveryStateService,
            InvitationEmailSender emailSender,
            InvitationAcceptUrlBuilder acceptUrlBuilder,
            InvitationEmailFailureSanitizer failureSanitizer) {
        this.accessGuard = accessGuard;
        this.creationService = creationService;
        this.deliveryStateService = deliveryStateService;
        this.emailSender = emailSender;
        this.acceptUrlBuilder = acceptUrlBuilder;
        this.failureSanitizer = failureSanitizer;
    }

    public OrganizationMemberInvitationResponse invite(
            UUID pathOrganizationId,
            InviteOrganizationMemberRequest request,
            UUID callerUserId) {
        TenantContext context = accessGuard.requireOrganizationAdmin(
                callerUserId, pathOrganizationId);

        CreatedOrganizationMemberInvitation created = creationService.create(
                context.organizationId(),
                context.organizationName(),
                request,
                callerUserId);
        InvitationEntity invitation = created.invitation();
        String acceptUrl = acceptUrlBuilder.build(created.rawToken());

        InvitationEntity invitationWithDeliveryState = sendEmailAndRecordState(
                invitation, acceptUrl);

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
                        invitation.getId(), Instant.now());
            } catch (RuntimeException updateFailure) {
                LOGGER.error(
                        "Org invitation email sent but delivery state update failed. invitationId={}, exceptionType={}, message={}",
                        invitation.getId(),
                        updateFailure.getClass().getName(),
                        updateFailure.getMessage());
                return invitation;
            }
        } catch (RuntimeException deliveryFailure) {
            String sanitizedReason = failureSanitizer.sanitize(deliveryFailure);
            try {
                return deliveryStateService.markFailed(
                        invitation.getId(), sanitizedReason, Instant.now());
            } catch (RuntimeException updateFailure) {
                LOGGER.error(
                        "Org invitation email failed and delivery state update failed. invitationId={}, deliveryExceptionType={}, updateExceptionType={}, updateMessage={}",
                        invitation.getId(),
                        deliveryFailure.getClass().getName(),
                        updateFailure.getClass().getName(),
                        updateFailure.getMessage());
                return invitation;
            }
        }
    }

    private OrganizationMemberInvitationResponse response(
            InvitationEntity invitation,
            String acceptUrl) {
        return new OrganizationMemberInvitationResponse(
                invitation.getId(),
                invitation.getAdminEmail(),
                invitation.getInviteeFullName(),
                invitation.getIntendedRole(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getEmailDeliveryStatus(),
                invitation.getEmailSentAt(),
                acceptUrlBuilder.devLinkEnabled() ? acceptUrl : null
        );
    }
}
