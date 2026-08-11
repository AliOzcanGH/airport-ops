package com.aliozcan.airportops.report_service.internal;

import com.aliozcan.airportops.report_service.readmodel.OrganizationOperationalSummaryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Platform-only read path for a tenant's operational metrics (station count,
 * recent flight activity), assembled by report-service's own read models.
 * Called by iam-service's platform tenant-detail endpoint, never by an
 * end-user session — protected by
 * {@link com.aliozcan.airportops.report_service.config.InternalServiceSecretFilter},
 * not JWT auth.
 */
@RestController
@RequestMapping("/internal/organizations/{orgId}/operational-summary")
public class InternalOperationalSummaryController {

    private final OrganizationOperationalSummaryRepository operationalSummaryRepository;

    public InternalOperationalSummaryController(
            OrganizationOperationalSummaryRepository operationalSummaryRepository) {
        this.operationalSummaryRepository = operationalSummaryRepository;
    }

    @GetMapping
    public OperationalSummaryResponse get(@PathVariable UUID orgId) {
        return OperationalSummaryResponse.from(
                orgId, operationalSummaryRepository.findByOrganizationId(orgId).orElse(null));
    }
}
