package com.aliozcan.airportops.iam_service.app.report;

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
 * Session/Keycloak-authenticated proxy for the browser-facing "/app/reports/*"
 * routes: issues a short-lived IAM JWT for the caller (W8A mechanism) and
 * forwards the request to report-service as-is. Authorization (report:read
 * permission, org match) is enforced downstream by report-service from the
 * IAM JWT it receives, mirroring the audit-log proxy pattern.
 */
@Service
public class AppReportProxyService {

    private final IamTokenService iamTokenService;
    private final RestClient reportServiceRestClient;

    public AppReportProxyService(IamTokenService iamTokenService, RestClient reportServiceRestClient) {
        this.iamTokenService = iamTokenService;
        this.reportServiceRestClient = reportServiceRestClient;
    }

    public ResponseEntity<String> dailyFlights(Jwt keycloakJwt, String date) {
        String iamAccessToken = issueToken(keycloakJwt);
        UUID organizationId = organizationIdOf(iamAccessToken);
        return forward(reportServiceRestClient.get()
                .uri("/organizations/{orgId}/reports/daily-flights?date={date}", organizationId, date)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + iamAccessToken));
    }

    public ResponseEntity<String> gateUtilization(Jwt keycloakJwt, String date) {
        String iamAccessToken = issueToken(keycloakJwt);
        UUID organizationId = organizationIdOf(iamAccessToken);
        return forward(reportServiceRestClient.get()
                .uri("/organizations/{orgId}/reports/gate-utilization?date={date}", organizationId, date)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + iamAccessToken));
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
