package com.aliozcan.airportops.iam_service.app.flight;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/flights")
public class AppFlightProxyController {

    private final AppFlightProxyService proxyService;

    public AppFlightProxyController(AppFlightProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping
    public ResponseEntity<String> list(@AuthenticationPrincipal Jwt jwt) {
        return proxyService.listFlights(jwt);
    }

    @PostMapping
    public ResponseEntity<String> create(
            @AuthenticationPrincipal Jwt jwt, @RequestBody String requestBody) {
        return proxyService.createFlight(jwt, requestBody);
    }
}
