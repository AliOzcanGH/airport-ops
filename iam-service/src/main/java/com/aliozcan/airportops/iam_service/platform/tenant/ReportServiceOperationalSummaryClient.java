package com.aliozcan.airportops.iam_service.platform.tenant;

import com.aliozcan.airportops.iam_service.config.InternalServiceSecretProperties;
import com.aliozcan.airportops.iam_service.platform.tenant.dto.OperationalSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Reads a tenant's operational summary from report-service's read model for
 * the platform tenant-detail page. This is supplementary information, not a
 * security decision — unlike the fail-closed Token Relay gate check (W10),
 * a failure here degrades gracefully to {@code null} rather than failing the
 * whole tenant-detail request, so the platform admin still sees member data
 * even if report-service is down.
 */
@Component
public class ReportServiceOperationalSummaryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportServiceOperationalSummaryClient.class);

    private final RestClient reportServiceRestClient;
    private final InternalServiceSecretProperties secretProperties;

    public ReportServiceOperationalSummaryClient(
            RestClient reportServiceRestClient,
            InternalServiceSecretProperties secretProperties) {
        this.reportServiceRestClient = reportServiceRestClient;
        this.secretProperties = secretProperties;
    }

    public OperationalSummaryResponse fetch(UUID organizationId) {
        try {
            return reportServiceRestClient.get()
                    .uri("/internal/organizations/{orgId}/operational-summary", organizationId)
                    .header("X-Internal-Service-Secret", secretProperties.internalServiceSecret())
                    .retrieve()
                    .body(OperationalSummaryResponse.class);
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "Failed to fetch operational summary for organization {} from report-service",
                    organizationId, exception);
            return null;
        }
    }
}
