package com.aliozcan.airportops.iam_service.platform.audit;

import com.aliozcan.airportops.iam_service.auth.dto.IamTokenResponse;
import com.aliozcan.airportops.iam_service.auth.token.IamTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Session/Keycloak-authenticated proxy for the browser-facing
 * "/platform/audit-logs" route: issues a short-lived IAM JWT for the caller
 * and forwards to audit-service's platform-scoped endpoint as-is. No
 * workspace/permission check happens at this layer — audit-service enforces
 * PLATFORM workspace + tenant:read from the IAM JWT it receives, so a tenant
 * caller is rejected downstream (403 PLATFORM_ONLY), same delegation pattern
 * as the tenant-scoped proxies.
 */
@Service
public class PlatformAuditProxyService {

    private final IamTokenService iamTokenService;
    private final RestClient auditServiceRestClient;

    public PlatformAuditProxyService(IamTokenService iamTokenService, RestClient auditServiceRestClient) {
        this.iamTokenService = iamTokenService;
        this.auditServiceRestClient = auditServiceRestClient;
    }

    public ResponseEntity<String> listAuditLogs(Jwt keycloakJwt) {
        IamTokenResponse tokenResponse = iamTokenService.issueToken(keycloakJwt);
        String iamAccessToken = tokenResponse.iamAccessToken();
        return forward(auditServiceRestClient.get()
                .uri("/platform/audit-logs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + iamAccessToken));
    }

    private ResponseEntity<String> forward(RestClient.RequestHeadersSpec<?> request) {
        try {
            return request.retrieve().toEntity(String.class);
        } catch (RestClientResponseException exception) {
            return ResponseEntity.status(exception.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(exception.getResponseBodyAsString());
        }
    }
}
