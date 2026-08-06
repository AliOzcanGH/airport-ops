package com.aliozcan.airportops.iam_service.app.audit;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/audit-logs")
public class AppAuditProxyController {

    private final AppAuditProxyService proxyService;

    public AppAuditProxyController(AppAuditProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping
    public ResponseEntity<String> list(@AuthenticationPrincipal Jwt jwt) {
        return proxyService.listAuditLogs(jwt);
    }
}
