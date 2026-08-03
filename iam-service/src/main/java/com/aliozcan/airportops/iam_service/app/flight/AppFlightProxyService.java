package com.aliozcan.airportops.iam_service.app.flight;

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
 * Session/Keycloak-authenticated proxy for the browser-facing "/app/flights"
 * routes: issues a short-lived IAM JWT for the caller (W8A mechanism) and
 * forwards the request to flight-service as-is. This layer intentionally
 * contains no flight business logic — authorization decisions, including the
 * gate Token Relay check (W10), are made downstream from the IAM JWT it
 * receives.
 */
@Service
public class AppFlightProxyService {

    private final IamTokenService iamTokenService;
    private final RestClient flightServiceRestClient;

    public AppFlightProxyService(
            IamTokenService iamTokenService, RestClient flightServiceRestClient) {
        this.iamTokenService = iamTokenService;
        this.flightServiceRestClient = flightServiceRestClient;
    }

    public ResponseEntity<String> listFlights(Jwt keycloakJwt) {
        String iamAccessToken = issueToken(keycloakJwt);
        UUID organizationId = organizationIdOf(iamAccessToken);
        return forward(flightServiceRestClient.get()
                .uri("/organizations/{orgId}/flights", organizationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + iamAccessToken));
    }

    public ResponseEntity<String> createFlight(Jwt keycloakJwt, String requestBody) {
        String iamAccessToken = issueToken(keycloakJwt);
        UUID organizationId = organizationIdOf(iamAccessToken);
        return forward(flightServiceRestClient.post()
                .uri("/organizations/{orgId}/flights", organizationId)
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
