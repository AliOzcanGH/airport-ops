package com.aliozcan.airportops.airport_service.station;

import com.aliozcan.airportops.airport_service.security.IamPrincipal;
import com.aliozcan.airportops.airport_service.station.dto.CreateStationRequest;
import com.aliozcan.airportops.airport_service.station.dto.StationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/stations")
    @PreAuthorize("hasAuthority('station:read')")
    public ResponseEntity<List<StationResponse>> list(
            @PathVariable UUID orgId, Authentication authentication) {
        return ResponseEntity.ok(stationService.list(orgId, principal(authentication)));
    }

    @PostMapping("/stations")
    @PreAuthorize("hasAuthority('station:create')")
    public ResponseEntity<StationResponse> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateStationRequest request,
            Authentication authentication) {
        StationResponse response = stationService.create(orgId, principal(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private IamPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof IamPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing IAM principal details");
    }
}
