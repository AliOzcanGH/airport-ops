package com.aliozcan.airportops.iam_service.app.setup.dto;

import com.aliozcan.airportops.iam_service.domain.model.OrganizationSetupProfileEntity;

import java.time.Instant;
import java.util.UUID;

public record AppSetupProfileResponse(
        UUID organizationId,
        String displayName,
        String iataCode,
        String icaoCode,
        String countryCode,
        String timezone,
        String baseAirportIata,
        String operationsContactEmail,
        Instant createdAt,
        Instant updatedAt
) {
    public static AppSetupProfileResponse from(OrganizationSetupProfileEntity profile) {
        return new AppSetupProfileResponse(
                profile.getOrganizationId(),
                profile.getDisplayName(),
                profile.getIataCode(),
                profile.getIcaoCode(),
                profile.getCountryCode(),
                profile.getTimezone(),
                profile.getBaseAirportIata(),
                profile.getOperationsContactEmail(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
