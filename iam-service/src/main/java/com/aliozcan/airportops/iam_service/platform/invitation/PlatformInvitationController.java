package com.aliozcan.airportops.iam_service.platform.invitation;

import com.aliozcan.airportops.iam_service.auth.UserNotProvisionedException;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.CreatePlatformInvitationRequest;
import com.aliozcan.airportops.iam_service.platform.invitation.dto.PlatformInvitationResponse;
import com.aliozcan.airportops.iam_service.security.IamAuthenticationDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/platform/invitations")
public class PlatformInvitationController {

    private final PlatformInvitationService platformInvitationService;

    public PlatformInvitationController(
            PlatformInvitationService platformInvitationService) {
        this.platformInvitationService = platformInvitationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('platform:invitation:create')")
    public ResponseEntity<PlatformInvitationResponse> create(
            @Valid @RequestBody CreatePlatformInvitationRequest request,
            Authentication authentication) {
        PlatformInvitationResponse response = platformInvitationService.create(
                request,
                iamUserId(authentication)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID iamUserId(Authentication authentication) {
        if (authentication != null
                && authentication.getDetails() instanceof IamAuthenticationDetails details
                && details.provisioned()
                && details.iamUserId() != null) {
            return details.iamUserId();
        }
        throw new UserNotProvisionedException();
    }
}
