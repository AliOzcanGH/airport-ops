package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.domain.model.enums.UserStatus;
import com.aliozcan.airportops.iam_service.keycloak.KeycloakProvisioningClient;
import com.aliozcan.airportops.iam_service.keycloak.KeycloakProvisioningException;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.AcceptInvitationRequest;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.InvitationAcceptanceResponse;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.ProvisioningStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InvitationAcceptanceService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InvitationAcceptanceService.class);

    private static final String READY_MESSAGE =
            "Invitation accepted. You can now sign in.";
    private static final String LOGIN_PENDING_MESSAGE =
            "Invitation accepted, but login setup is not ready yet. "
                    + "Please contact platform support.";

    private final InvitationAcceptanceTransactionService transactionService;
    private final KeycloakProvisioningClient keycloakProvisioningClient;
    private final UserProvisioningStateService userProvisioningStateService;

    public InvitationAcceptanceService(
            InvitationAcceptanceTransactionService transactionService,
            KeycloakProvisioningClient keycloakProvisioningClient,
            UserProvisioningStateService userProvisioningStateService) {
        this.transactionService = transactionService;
        this.keycloakProvisioningClient = keycloakProvisioningClient;
        this.userProvisioningStateService = userProvisioningStateService;
    }

    public InvitationAcceptanceResponse accept(
            AcceptInvitationRequest request) {
        IamProvisioningResult provisioned = transactionService.provision(
                request.token(),
                request.fullName());

        try {
            String keycloakUserId = keycloakProvisioningClient.createUser(
                    provisioned.email(),
                    request.fullName(),
                    request.password());
            userProvisioningStateService.activate(
                    provisioned.userId(),
                    keycloakUserId);
            return response(
                    provisioned,
                    UserStatus.ACTIVE,
                    ProvisioningStatus.READY,
                    READY_MESSAGE);
        } catch (KeycloakProvisioningException exception) {
            LOGGER.warn(
                    "Keycloak provisioning failed for IAM user {}: {} - {}",
                    provisioned.userId(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
            userProvisioningStateService.markKeycloakSyncFailed(
                    provisioned.userId());
            return response(
                    provisioned,
                    UserStatus.KEYCLOAK_SYNC_FAILED,
                    ProvisioningStatus.LOGIN_SETUP_PENDING,
                    LOGIN_PENDING_MESSAGE);
        }
    }

    private InvitationAcceptanceResponse response(
            IamProvisioningResult provisioned,
            UserStatus userStatus,
            ProvisioningStatus provisioningStatus,
            String message) {
        return new InvitationAcceptanceResponse(
                provisioned.email(),
                provisioned.organizationName(),
                provisioned.organizationStatus(),
                userStatus,
                provisioningStatus,
                message
        );
    }
}
