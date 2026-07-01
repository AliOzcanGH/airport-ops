package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.platform.invitation.InvitationTokenService.GeneratedInvitationToken;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.CreatePlatformInvitationRequest;
import com.aliozcan.airportops.iam_service.repository.InvitationRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformInvitationServiceTests {

    private InvitationRepository invitationRepository;
    private InvitationTokenService invitationTokenService;
    private PlatformInvitationService service;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(InvitationRepository.class);
        invitationTokenService = mock(InvitationTokenService.class);
        service = new PlatformInvitationService(
                invitationRepository,
                invitationTokenService);

        when(invitationRepository.existsPendingByAdminEmail("admin@pegasus.demo"))
                .thenReturn(false);
        when(invitationTokenService.generate())
                .thenReturn(new GeneratedInvitationToken("raw-token", "token-hash"));
    }

    @Test
    void convertsPendingEmailConstraintViolationToBusinessConflict() {
        DataIntegrityViolationException failure = dataIntegrityFailure(
                "uq_invitations_pending_admin_email");
        when(invitationRepository.saveAndFlush(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.create(request(), UUID.randomUUID()))
                .isInstanceOf(PendingInvitationExistsException.class);
    }

    @Test
    void doesNotHideOtherDataIntegrityViolations() {
        DataIntegrityViolationException failure = dataIntegrityFailure(
                "uq_invitations_token_hash");
        when(invitationRepository.saveAndFlush(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.create(request(), UUID.randomUUID()))
                .isSameAs(failure);
    }

    private DataIntegrityViolationException dataIntegrityFailure(String constraintName) {
        ConstraintViolationException constraintFailure = new ConstraintViolationException(
                "constraint violation",
                new SQLException("duplicate", "23505"),
                constraintName);
        return new DataIntegrityViolationException("save failed", constraintFailure);
    }

    private CreatePlatformInvitationRequest request() {
        return new CreatePlatformInvitationRequest(
                "admin@pegasus.demo",
                "Pegasus Airlines");
    }
}
