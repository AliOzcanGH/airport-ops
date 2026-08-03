package com.aliozcan.airportops.iam_service.app.gate;

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
 * Session/Keycloak-authenticated proxy for the browser-facing "/app/stations/{stationId}/gates"
 * routes: issues a short-lived IAM JWT for the caller (W8A mechanism) and forwards the request
 * to airport-service as-is. This layer intentionally contains no gate business logic —
 * authorization decisions, including station-ownership verification, are made by
 * airport-service itself from the IAM JWT it receives.
 */
@Service
public class AppGateProxyService {

    private final IamTokenService iamTokenService;
    private final RestClient airportServiceRestClient;

    public AppGateProxyService(
            IamTokenService iamTokenService, RestClient airportServiceRestClient) {
        this.iamTokenService = iamTokenService;
        this.airportServiceRestClient = airportServiceRestClient;
    }

    public ResponseEntity<String> listGates(Jwt keycloakJwt, UUID stationId) {
        String iamAccessToken = issueToken(keycloakJwt);
        UUID organizationId = organizationIdOf(iamAccessToken);
        return forward(airportServiceRestClient.get()
                .uri("/organizations/{orgId}/stations/{stationId}/gates", organizationId, stationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + iamAccessToken));
    }

    public ResponseEntity<String> createGate(Jwt keycloakJwt, UUID stationId, String requestBody) {
        String iamAccessToken = issueToken(keycloakJwt);
        UUID organizationId = organizationIdOf(iamAccessToken);
        return forward(airportServiceRestClient.post()
                .uri("/organizations/{orgId}/stations/{stationId}/gates", organizationId, stationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + iamAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody));
    }

    public ResponseEntity<String> updateGateStatus(
            Jwt keycloakJwt, UUID stationId, UUID gateId, String requestBody) {
        String iamAccessToken = issueToken(keycloakJwt);
        UUID organizationId = organizationIdOf(iamAccessToken);
        return forward(airportServiceRestClient.put()
                .uri("/organizations/{orgId}/stations/{stationId}/gates/{gateId}/status",
                        organizationId, stationId, gateId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + iamAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody));
    }

    private String issueToken(Jwt keycloakJwt) {
        IamTokenResponse tokenResponse = iamTokenService.issueToken(keycloakJwt);
        return tokenResponse.iamAccessToken();
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
