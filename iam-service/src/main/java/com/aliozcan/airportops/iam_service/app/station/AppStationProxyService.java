package com.aliozcan.airportops.iam_service.app.station;

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
 * Session/Keycloak-authenticated proxy for the browser-facing "/app/stations"
 * route: issues a short-lived IAM JWT for the caller (W8A mechanism) and
 * forwards the request to airport-service as-is. This layer intentionally
 * contains no station business logic — authorization decisions are made by
 * airport-service itself from the IAM JWT it receives.
 */
@Service
public class AppStationProxyService {

    private final IamTokenService iamTokenService;
    private final RestClient airportServiceRestClient;

    public AppStationProxyService(
            IamTokenService iamTokenService, RestClient airportServiceRestClient) {
        this.iamTokenService = iamTokenService;
        this.airportServiceRestClient = airportServiceRestClient;
    }

    public ResponseEntity<String> listStations(Jwt keycloakJwt) {
        IamTokenResponse tokenResponse = iamTokenService.issueToken(keycloakJwt);
        UUID organizationId = organizationIdOf(tokenResponse.iamAccessToken());

        try {
            ResponseEntity<String> response = airportServiceRestClient.get()
                    .uri("/organizations/{orgId}/stations", organizationId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.iamAccessToken())
                    .retrieve()
                    .toEntity(String.class);
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (RestClientResponseException exception) {
            return ResponseEntity.status(exception.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(exception.getResponseBodyAsString());
        }
    }

    public ResponseEntity<String> createStation(Jwt keycloakJwt, String requestBody) {
        IamTokenResponse tokenResponse = iamTokenService.issueToken(keycloakJwt);
        UUID organizationId = organizationIdOf(tokenResponse.iamAccessToken());

        try {
            ResponseEntity<String> response = airportServiceRestClient.post()
                    .uri("/organizations/{orgId}/stations", organizationId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.iamAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);
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
