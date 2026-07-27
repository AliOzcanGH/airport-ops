package com.aliozcan.airportops.iam_service.tenant.member;

import com.aliozcan.airportops.iam_service.auth.UserNotProvisionedException;
import com.aliozcan.airportops.iam_service.platform.tenant.dto.PlatformTenantMemberResponse;
import com.aliozcan.airportops.iam_service.security.IamAuthenticationDetails;
import com.aliozcan.airportops.iam_service.tenant.member.dto.InviteOrganizationMemberRequest;
import com.aliozcan.airportops.iam_service.tenant.member.dto.OrganizationMemberInvitationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/organizations/{orgId}")
public class TenantMemberController {

    private final TenantMemberInvitationService invitationService;
    private final TenantMemberListService listService;

    public TenantMemberController(
            TenantMemberInvitationService invitationService,
            TenantMemberListService listService) {
        this.invitationService = invitationService;
        this.listService = listService;
    }

    @PostMapping("/invitations")
    public ResponseEntity<OrganizationMemberInvitationResponse> invite(
            @PathVariable UUID orgId,
            @Valid @RequestBody InviteOrganizationMemberRequest request,
            Authentication authentication) {
        OrganizationMemberInvitationResponse response = invitationService.invite(
                orgId, request, iamUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/members")
    public List<PlatformTenantMemberResponse> members(
            @PathVariable UUID orgId,
            Authentication authentication) {
        return listService.list(orgId, iamUserId(authentication));
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
