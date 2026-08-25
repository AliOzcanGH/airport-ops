package com.aliozcan.airportops.iam_service.app.audit;

import com.aliozcan.airportops.iam_service.auth.dto.IamTokenResponse;
import com.aliozcan.airportops.iam_service.auth.token.IamTokenService;
import com.aliozcan.airportops.iam_service.auth.token.NoWorkspaceContextException;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.text.ParseException;
import java.util.UUID;

/**
 * Session/Keycloak-authenticated proxy for the browser-facing "/app/audit-logs"
 * route: issues a short-lived IAM JWT for the caller (W8A mechanism) and
 * forwards the request to audit-service as-is. Authorization (audit:read
 * permission, org match) is enforced downstream by audit-service from the
 * IAM JWT it receives, mirroring the flight-service proxy pattern.
 */
@Service
public class AppAuditProxyService {

    private final IamTokenService iamTokenService;
    private final RestClient auditServiceRestClient;

    public AppAuditProxyService(IamTokenService iamTokenService, RestClient auditServiceRestClient) {
        this.iamTokenService = iamTokenService;
        this.auditServiceRestClient = auditServiceRestClient;
    }

    public ResponseEntity<String> listAuditLogs(Jwt keycloakJwt) {
        String iamAccessToken = issueToken(keycloakJwt);
        UUID organizationId = organizationIdOf(iamAccessToken);
        return forward(auditServiceRestClient.get()
                .uri("/organizations/{orgId}/audit-logs", organizationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + iamAccessToken));
    }

    private String issueToken(Jwt keycloakJwt) {
        IamTokenResponse tokenResponse = iamTokenService.issueToken(keycloakJwt);
        return tokenResponse.iamAccessToken();
    }

    private ResponseEntity<String> forward(RestClient.RequestHeadersSpec<?> request) {
        try {
            ResponseEntity<String> response = request.retrieve().toEntity(String.class);
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (RestClientResponseException exception) {
            return ResponseEntity.status(exception.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(exception.getResponseBodyAsString());
        }
    }

    private UUID organizationIdOf(String iamAccessToken) {
        try {
            String organizationId = SignedJWT.parse(iamAccessToken)
                    .getJWTClaimsSet()
                    .getStringClaim("organizationId");
            if (organizationId == null) {
                throw new NoWorkspaceContextException();
            }
            return UUID.fromString(organizationId);
        } catch (ParseException exception) {
            throw new IllegalStateException("Failed to parse issued IAM token", exception);
        }
    }
}
