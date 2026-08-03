package com.aliozcan.airportops.airport_service.gate;

import com.aliozcan.airportops.airport_service.gate.dto.GateResponse;
import com.aliozcan.airportops.airport_service.security.IamPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Flat, station-agnostic gate lookup used by other services' Token Relay
 * calls (e.g. flight-service verifying an assigned gate) that only have a
 * gate id on hand, not the owning station id.
 */
@RestController
@RequestMapping("/organizations/{orgId}/gates")
public class GateLookupController {

    private final GateService gateService;

    public GateLookupController(GateService gateService) {
        this.gateService = gateService;
    }

    @GetMapping("/{gateId}")
    @PreAuthorize("hasAuthority('gate:read')")
    public ResponseEntity<GateResponse> getOne(
            @PathVariable UUID orgId, @PathVariable UUID gateId, Authentication authentication) {
        return ResponseEntity.ok(gateService.getOneByOrganization(orgId, gateId, principal(authentication)));
    }

    private IamPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof IamPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing IAM principal details");
    }
}
