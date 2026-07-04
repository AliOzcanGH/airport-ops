package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.domain.model.InvitationEntity;
import com.aliozcan.airportops.iam_service.domain.model.enums.InvitationStatus;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.InvitationValidationResponse;
import com.aliozcan.airportops.iam_service.repository.InvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitationValidationServiceTests {

    private static final String RAW_TOKEN = "A".repeat(43);
    private static final String TOKEN_HASH = "token-hash";

    private InvitationRepository invitationRepository;
    private InvitationTokenService invitationTokenService;
    private InvitationEmailMasker invitationEmailMasker;
    private InvitationValidationService service;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(InvitationRepository.class);
        invitationTokenService = mock(InvitationTokenService.class);
        invitationEmailMasker = mock(InvitationEmailMasker.class);
        service = new InvitationValidationService(
                invitationRepository,
                invitationTokenService,
                invitationEmailMasker);

        when(invitationTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    }

    @Test
    void validatesPendingInvitationWithoutWriting() {
        InvitationEntity invitation = invitation(
                InvitationStatus.PENDING,
                Instant.now().plusSeconds(3600));
        when(invitationRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(invitation));
        when(invitationEmailMasker.mask("admin@pegasus.demo"))
                .thenReturn("ad***@pegasus.demo");

        InvitationValidationResponse response = service.validate(RAW_TOKEN);

        assertThat(response.organizationName()).isEqualTo("Pegasus Airlines");
        assertThat(response.invitedEmail()).isEqualTo("ad***@pegasus.demo");
        assertThat(response.expiresAt()).isEqualTo(invitation.getExpiresAt());
        verify(invitationRepository, never()).save(any());
        verify(invitationRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsUnknownInvitation() {
        when(invitationRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate(RAW_TOKEN))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void hidesCancelledInvitation() {
        assertStatusFailure(
                InvitationStatus.CANCELLED,
                Instant.now().plusSeconds(3600),
                InvitationNotFoundException.class);
    }

    @Test
    void rejectsAcceptedInvitation() {
        assertStatusFailure(
                InvitationStatus.ACCEPTED,
                Instant.now().plusSeconds(3600),
                InvitationAlreadyUsedException.class);
    }

    @Test
    void rejectsExplicitlyExpiredInvitation() {
        assertStatusFailure(
                InvitationStatus.EXPIRED,
                Instant.now().plusSeconds(3600),
                InvitationExpiredException.class);
    }

    @Test
    void rejectsPendingInvitationPastExpiration() {
        assertStatusFailure(
                InvitationStatus.PENDING,
                Instant.now().minusSeconds(3600),
                InvitationExpiredException.class);
    }

    private void assertStatusFailure(
            InvitationStatus status,
            Instant expiresAt,
            Class<? extends RuntimeException> exceptionType) {
        InvitationEntity invitation = invitation(status, expiresAt);
        when(invitationRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.validate(RAW_TOKEN))
                .isInstanceOf(exceptionType);
    }

    private InvitationEntity invitation(
            InvitationStatus status,
            Instant expiresAt) {
        InvitationEntity invitation = mock(InvitationEntity.class);
        when(invitation.getStatus()).thenReturn(status);
        when(invitation.getExpiresAt()).thenReturn(expiresAt);
        when(invitation.getCompanyName()).thenReturn("Pegasus Airlines");
        when(invitation.getAdminEmail()).thenReturn("admin@pegasus.demo");
        return invitation;
    }
}
