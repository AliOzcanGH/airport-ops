package com.aliozcan.airportops.iam_service.app.report;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/reports")
public class AppReportProxyController {

    private final AppReportProxyService proxyService;

    public AppReportProxyController(AppReportProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping("/daily-flights")
    public ResponseEntity<String> dailyFlights(@RequestParam String date, @AuthenticationPrincipal Jwt jwt) {
        return proxyService.dailyFlights(jwt, date);
    }

    @GetMapping("/gate-utilization")
    public ResponseEntity<String> gateUtilization(@RequestParam String date, @AuthenticationPrincipal Jwt jwt) {
        return proxyService.gateUtilization(jwt, date);
    }
}
