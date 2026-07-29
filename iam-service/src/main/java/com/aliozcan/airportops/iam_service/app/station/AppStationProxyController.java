package com.aliozcan.airportops.iam_service.app.station;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/stations")
public class AppStationProxyController {

    private final AppStationProxyService proxyService;

    public AppStationProxyController(AppStationProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @PostMapping
    public ResponseEntity<String> create(
            @AuthenticationPrincipal Jwt jwt, @RequestBody String requestBody) {
        return proxyService.createStation(jwt, requestBody);
    }
}
