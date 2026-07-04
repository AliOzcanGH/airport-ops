package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.platform.invitation.dto.InvitationValidationResponse;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.ValidateInvitationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invitations")
public class InvitationValidationController {

    private final InvitationValidationService invitationValidationService;

    public InvitationValidationController(
            InvitationValidationService invitationValidationService) {
        this.invitationValidationService = invitationValidationService;
    }

    @PostMapping("/validate")
    public ResponseEntity<InvitationValidationResponse> validate(
            @Valid @RequestBody ValidateInvitationRequest request) {
        return ResponseEntity.ok(
                invitationValidationService.validate(request.token()));
    }
}
