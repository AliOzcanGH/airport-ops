package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.platform.invitation.dto.AcceptInvitationRequest;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.InvitationAcceptanceResponse;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.ProvisioningStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invitations")
public class InvitationAcceptanceController {

    private final InvitationAcceptanceService invitationAcceptanceService;

    public InvitationAcceptanceController(
            InvitationAcceptanceService invitationAcceptanceService) {
        this.invitationAcceptanceService = invitationAcceptanceService;
    }

    @PostMapping("/accept")
    public ResponseEntity<InvitationAcceptanceResponse> accept(
            @Valid @RequestBody AcceptInvitationRequest request) {
        InvitationAcceptanceResponse response =
                invitationAcceptanceService.accept(request);
        HttpStatus status = response.provisioningStatus()
                == ProvisioningStatus.READY
                ? HttpStatus.CREATED
                : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(response);
    }
}
