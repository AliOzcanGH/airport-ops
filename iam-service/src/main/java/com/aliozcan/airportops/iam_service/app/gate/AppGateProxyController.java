package com.aliozcan.airportops.iam_service.app.gate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/app/stations/{stationId}/gates")
public class AppGateProxyController {

    private final AppGateProxyService proxyService;

    public AppGateProxyController(AppGateProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping
    public ResponseEntity<String> list(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID stationId) {
        return proxyService.listGates(jwt, stationId);
    }

    @PostMapping
    public ResponseEntity<String> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID stationId,
            @RequestBody String requestBody) {
        return proxyService.createGate(jwt, stationId, requestBody);
    }

    @PutMapping("/{gateId}/status")
    public ResponseEntity<String> updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID stationId,
            @PathVariable UUID gateId,
            @RequestBody String requestBody) {
        return proxyService.updateGateStatus(jwt, stationId, gateId, requestBody);
    }
}
