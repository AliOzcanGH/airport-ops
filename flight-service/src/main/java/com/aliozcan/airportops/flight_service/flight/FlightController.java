package com.aliozcan.airportops.flight_service.flight;

import com.aliozcan.airportops.flight_service.flight.dto.CreateFlightRequest;
import com.aliozcan.airportops.flight_service.flight.dto.FlightResponse;
import com.aliozcan.airportops.flight_service.security.IamPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
@RequestMapping("/organizations/{orgId}/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('flight:read')")
    public ResponseEntity<List<FlightResponse>> list(
            @PathVariable UUID orgId, Authentication authentication) {
        return ResponseEntity.ok(flightService.list(orgId, principal(authentication)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('flight:create')")
    public ResponseEntity<FlightResponse> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateFlightRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        String rawAuthorizationHeader = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        FlightResponse response =
                flightService.create(orgId, principal(authentication), request, rawAuthorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private IamPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof IamPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing IAM principal details");
    }
}
