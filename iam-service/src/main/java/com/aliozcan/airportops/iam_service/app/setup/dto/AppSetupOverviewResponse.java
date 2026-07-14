package com.aliozcan.airportops.iam_service.app.setup.dto;

import com.aliozcan.airportops.iam_service.domain.model.enums.OrganizationStatus;
import com.aliozcan.airportops.iam_service.domain.model.enums.PreferredLanguage;

import java.util.List;
import java.util.UUID;

public record AppSetupOverviewResponse(
        UUID organizationId,
        String organizationName,
        OrganizationStatus organizationStatus,
        PreferredLanguage preferredLanguage,
        List<AppSetupStepResponse> steps
) {
}
