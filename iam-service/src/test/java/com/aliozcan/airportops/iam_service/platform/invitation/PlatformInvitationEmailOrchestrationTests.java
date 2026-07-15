package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.domain.model.InvitationEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationEmailDeliveryStatus;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.CreatePlatformInvitationRequest;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.PlatformInvitationResponse;
import com.aliozcan.airportops.iam_service.platform.invitation.email.InvitationAcceptUrlBuilder;
import com.aliozcan.airportops.iam_service.platform.invitation.email.InvitationEmailFailureSanitizer;
import com.aliozcan.airportops.iam_service.platform.invitation.email.InvitationEmailMessage;
import com.aliozcan.airportops.iam_service.platform.invitation.email.InvitationEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformInvitationEmailOrchestrationTests {

    private PlatformInvitationCreationTransactionService creationService;
    private InvitationEmailDeliveryStateService deliveryStateService;
    private InvitationEmailSender emailSender;
    private InvitationAcceptUrlBuilder acceptUrlBuilder;
    private InvitationEmailFailureSanitizer failureSanitizer;
    private PlatformInvitationService service;

    @BeforeEach
    void setUp() {
        creationService = mock(PlatformInvitationCreationTransactionService.class);
        deliveryStateService = mock(InvitationEmailDeliveryStateService.class);
        emailSender = mock(InvitationEmailSender.class);
        acceptUrlBuilder = mock(InvitationAcceptUrlBuilder.class);
        failureSanitizer = new InvitationEmailFailureSanitizer();
        service = new PlatformInvitationService(
                creationService,
                deliveryStateService,
                emailSender,
                acceptUrlBuilder,
                failureSanitizer);
    }

    @Test
    void sendsEmailAndReturnsSentDeliveryStateWithDevLink() {
        InvitationEntity createdInvitation = pendingInvitation();
        InvitationEntity sentInvitation = pendingInvitation();
        sentInvitation.markEmailSent(Instant.parse("2026-07-14T10:00:00Z"));
        when(creationService.create(any(), any()))
                .thenReturn(new CreatedPlatformInvitation(
                        createdInvitation,
                        "abcdefghijklmnopqrstuvwxyzABCDEFGH_12345678"));
        when(acceptUrlBuilder.build("abcdefghijklmnopqrstuvwxyzABCDEFGH_12345678"))
                .thenReturn("http://127.0.0.1:5173/invitations/accept?token=abcdefghijklmnopqrstuvwxyzABCDEFGH_12345678");
        when(acceptUrlBuilder.devLinkEnabled()).thenReturn(true);
        when(deliveryStateService.markSent(any(), any()))
                .thenReturn(sentInvitation);

        PlatformInvitationResponse response = service.create(
                new CreatePlatformInvitationRequest(
                        "admin@airline.demo",
                        "Airline Tenant"),
                UUID.randomUUID());

        var message = forClass(InvitationEmailMessage.class);
        verify(emailSender).send(message.capture());
        assertThat(message.getValue().recipientEmail()).isEqualTo("admin@airline.demo");
        assertThat(message.getValue().organizationName()).isEqualTo("Airline Tenant");
        assertThat(message.getValue().acceptUrl()).contains("/invitations/accept?token=");
        assertThat(response.emailDeliveryStatus())
                .isEqualTo(InvitationEmailDeliveryStatus.SENT);
        assertThat(response.emailSentAt()).isNotNull();
        assertThat(response.devAcceptLink()).contains("/invitations/accept?token=");
    }

    @Test
    void emailFailureReturnsFailedDeliveryStateWithoutExposingFailureReason() {
        InvitationEntity createdInvitation = pendingInvitation();
        InvitationEntity failedInvitation = pendingInvitation();
        failedInvitation.markEmailFailed("safe failure", Instant.parse("2026-07-14T10:00:00Z"));
        when(creationService.create(any(), any()))
                .thenReturn(new CreatedPlatformInvitation(createdInvitation, "raw-token"));
        when(acceptUrlBuilder.build("raw-token"))
                .thenReturn("http://127.0.0.1:5173/invitations/accept?token=raw-token");
        when(acceptUrlBuilder.devLinkEnabled()).thenReturn(false);
        doThrow(new RuntimeException("provider failure"))
                .when(emailSender)
                .send(any());
        when(deliveryStateService.markFailed(any(), any(), any()))
                .thenReturn(failedInvitation);

        PlatformInvitationResponse response = service.create(
                new CreatePlatformInvitationRequest(
                        "admin@airline.demo",
                        "Airline Tenant"),
                UUID.randomUUID());

        assertThat(response.emailDeliveryStatus())
                .isEqualTo(InvitationEmailDeliveryStatus.FAILED);
        assertThat(response.emailSentAt()).isNull();
        assertThat(response.devAcceptLink()).isNull();
    }

    private InvitationEntity pendingInvitation() {
        Instant now = Instant.parse("2026-07-14T09:00:00Z");
        return InvitationEntity.pending(
                "Airline Tenant",
                "admin@airline.demo",
                "token-hash",
                UUID.randomUUID(),
                now,
                now.plusSeconds(259200));
    }
}
