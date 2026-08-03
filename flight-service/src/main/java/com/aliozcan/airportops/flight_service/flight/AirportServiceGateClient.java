package com.aliozcan.airportops.flight_service.flight;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Token Relay (W10, K4.5 pattern): forwards the caller's own IAM JWT to
 * airport-service unchanged — no new token is minted here — so that
 * airport-service can apply its own tenant/ownership authorization to the
 * gate lookup.
 */
@Component
public class AirportServiceGateClient {

    private final RestClient airportServiceRestClient;

    public AirportServiceGateClient(RestClient airportServiceRestClient) {
        this.airportServiceRestClient = airportServiceRestClient;
    }

    public GateSnapshot fetchGate(UUID organizationId, UUID gateId, String rawAuthorizationHeader) {
        try {
            GateSnapshot snapshot = airportServiceRestClient.get()
                    .uri("/organizations/{orgId}/gates/{gateId}", organizationId, gateId)
                    .header(HttpHeaders.AUTHORIZATION, rawAuthorizationHeader)
                    .retrieve()
                    .body(GateSnapshot.class);
            if (snapshot == null) {
                throw new GateNotFoundException();
            }
            return snapshot;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new GateNotFoundException();
        } catch (HttpClientErrorException exception) {
            // Any other 4xx (403 tenant mismatch, 401, etc.) is treated as fail-closed:
            // the gate cannot be verified as belonging to this organization.
            throw new GateNotFoundException();
        } catch (HttpServerErrorException | ResourceAccessException exception) {
            // airport-service reachable-but-erroring, or unreachable entirely
            // (connection refused / timeout) — must not be silently treated as valid.
            throw new GateVerificationUnavailableException(exception);
        }
    }
}
