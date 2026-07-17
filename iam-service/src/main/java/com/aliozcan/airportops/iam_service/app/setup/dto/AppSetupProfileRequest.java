package com.aliozcan.airportops.iam_service.app.setup.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public record AppSetupProfileRequest(
        @NotBlank @Size(min = 2, max = 160) String displayName,
        @Pattern(regexp = "^[A-Z0-9]{2}$") String iataCode,
        @Pattern(regexp = "^[A-Z]{3}$") String icaoCode,
        @Pattern(regexp = "^[A-Z]{2}$") String countryCode,
        @Size(max = 80) String timezone,
        @Pattern(regexp = "^[A-Z]{3}$") String baseAirportIata,
        @Email @Size(max = 254) String operationsContactEmail
) {
    private static final Set<String> ISO_COUNTRIES = Set.copyOf(
            Arrays.asList(Locale.getISOCountries()));

    public AppSetupProfileRequest {
        displayName = trim(displayName);
        iataCode = uppercaseOptional(iataCode);
        icaoCode = uppercaseOptional(icaoCode);
        countryCode = uppercaseOptional(countryCode);
        timezone = optional(timezone);
        baseAirportIata = uppercaseOptional(baseAirportIata);
        operationsContactEmail = lowercaseOptional(operationsContactEmail);
    }

    @AssertTrue
    public boolean isCountryCodeValid() {
        return countryCode == null || ISO_COUNTRIES.contains(countryCode);
    }

    @AssertTrue
    public boolean isTimezoneValid() {
        if (timezone == null) {
            return true;
        }
        try {
            ZoneId.of(timezone);
            return true;
        } catch (DateTimeException exception) {
            return false;
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String optional(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private static String uppercaseOptional(String value) {
        String normalized = optional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String lowercaseOptional(String value) {
        String normalized = optional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
