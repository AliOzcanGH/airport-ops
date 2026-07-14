package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;
import com.aliozcan.airportops.iam_service.domain.model.enums.UserStatus;
import com.aliozcan.airportops.iam_service.keycloak.KeycloakProvisioningClient;
import com.aliozcan.airportops.iam_service.keycloak.KeycloakProvisioningException;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.AcceptInvitationRequest;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.InvitationAcceptanceResponse;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.ProvisioningStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitationAcceptanceServiceTests {

    private static final UUID USER_ID = UUID.randomUUID();

    private InvitationAcceptanceTransactionService transactionService;
    private KeycloakProvisioningClient keycloakProvisioningClient;
    private UserProvisioningStateService stateService;
    private InvitationAcceptanceService service;

    @BeforeEach
    void setUp() {
        transactionService = mock(InvitationAcceptanceTransactionService.class);
        keycloakProvisioningClient = mock(KeycloakProvisioningClient.class);
        stateService = mock(UserProvisioningStateService.class);
        service = new InvitationAcceptanceService(
                transactionService,
                keycloakProvisioningClient,
                stateService);
        when(transactionService.provision("A".repeat(43), "Airline Admin", "TR"))
                .thenReturn(provisioned());
    }

    @Test
    void activatesUserAfterKeycloakSuccess() {
        when(keycloakProvisioningClient.createUser(
                "admin@pegasus.demo",
                "Airline Admin",
                "StrongPassword123!"))
                .thenReturn("keycloak-subject");

        InvitationAcceptanceResponse response = service.accept(request());

        assertThat(response.userStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.provisioningStatus()).isEqualTo(ProvisioningStatus.READY);
        verify(stateService).activate(USER_ID, "keycloak-subject");
        verify(stateService, never()).markKeycloakSyncFailed(USER_ID);
    }

    @Test
    void marksUserFailedAfterKeycloakFailure() {
        when(keycloakProvisioningClient.createUser(
                "admin@pegasus.demo",
                "Airline Admin",
                "StrongPassword123!"))
                .thenThrow(new KeycloakProvisioningException(
                        "duplicate identity",
                        true));

        InvitationAcceptanceResponse response = service.accept(request());

        assertThat(response.userStatus())
                .isEqualTo(UserStatus.KEYCLOAK_SYNC_FAILED);
        assertThat(response.provisioningStatus())
                .isEqualTo(ProvisioningStatus.LOGIN_SETUP_PENDING);
        verify(stateService).markKeycloakSyncFailed(USER_ID);
        verify(stateService, never()).activate(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotMisclassifyIamActivationFailureAsKeycloakFailure() {
        DataAccessResourceFailureException failure =
                new DataAccessResourceFailureException("database unavailable");
        when(keycloakProvisioningClient.createUser(
                "admin@pegasus.demo",
                "Airline Admin",
                "StrongPassword123!"))
                .thenReturn("keycloak-subject");
        org.mockito.Mockito.doThrow(failure)
                .when(stateService)
                .activate(USER_ID, "keycloak-subject");

        assertThatThrownBy(() -> service.accept(request())).isSameAs(failure);

        verify(stateService, never()).markKeycloakSyncFailed(USER_ID);
    }

    private AcceptInvitationRequest request() {
        return new AcceptInvitationRequest(
                "A".repeat(43),
                "Airline Admin",
                "StrongPassword123!",
                "TR");
    }

    private IamProvisioningResult provisioned() {
        return new IamProvisioningResult(
                USER_ID,
                "admin@pegasus.demo",
                "Pegasus Airlines",
                OrganizationStatus.ONBOARDING_INCOMPLETE);
    }
}
