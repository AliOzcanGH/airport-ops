package com.aliozcan.airportops.airport_service.gate;

import com.aliozcan.airportops.airport_service.gate.dto.CreateGateRequest;
import com.aliozcan.airportops.airport_service.gate.dto.GateResponse;
import com.aliozcan.airportops.airport_service.gate.dto.UpdateGateStatusRequest;
import com.aliozcan.airportops.airport_service.security.IamPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/organizations/{orgId}/stations/{stationId}/gates")
public class GateController {

    private final GateService gateService;

    public GateController(GateService gateService) {
        this.gateService = gateService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('gate:read')")
    public ResponseEntity<List<GateResponse>> list(
            @PathVariable UUID orgId, @PathVariable UUID stationId, Authentication authentication) {
        return ResponseEntity.ok(gateService.list(orgId, stationId, principal(authentication)));
    }

    @GetMapping("/{gateId}")
    @PreAuthorize("hasAuthority('gate:read')")
    public ResponseEntity<GateResponse> getOne(
            @PathVariable UUID orgId,
            @PathVariable UUID stationId,
            @PathVariable UUID gateId,
            Authentication authentication) {
        return ResponseEntity.ok(gateService.getOne(orgId, stationId, gateId, principal(authentication)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('gate:update')")
    public ResponseEntity<GateResponse> create(
            @PathVariable UUID orgId,
            @PathVariable UUID stationId,
            @Valid @RequestBody CreateGateRequest request,
            Authentication authentication) {
        GateResponse response = gateService.create(orgId, stationId, principal(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{gateId}/status")
    @PreAuthorize("hasAuthority('gate:update')")
    public ResponseEntity<GateResponse> updateStatus(
            @PathVariable UUID orgId,
            @PathVariable UUID stationId,
            @PathVariable UUID gateId,
            @Valid @RequestBody UpdateGateStatusRequest request,
            Authentication authentication) {
        GateResponse response =
                gateService.updateStatus(orgId, stationId, gateId, principal(authentication), request);
        return ResponseEntity.ok(response);
    }

    private IamPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof IamPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing IAM principal details");
    }
}
