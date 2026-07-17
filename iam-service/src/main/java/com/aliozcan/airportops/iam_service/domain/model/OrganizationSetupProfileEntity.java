package com.aliozcan.airportops.iam_service.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_setup_profiles", schema = "iam")
public class OrganizationSetupProfileEntity {

    @Id
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "iata_code", length = 2)
    private String iataCode;

    @Column(name = "icao_code", length = 3)
    private String icaoCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(length = 80)
    private String timezone;

    @Column(name = "base_airport_iata", length = 3)
    private String baseAirportIata;

    @Column(name = "operations_contact_email", length = 254)
    private String operationsContactEmail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationSetupProfileEntity() {
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIataCode() {
        return iataCode;
    }

    public String getIcaoCode() {
        return icaoCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getBaseAirportIata() {
        return baseAirportIata;
    }

    public String getOperationsContactEmail() {
        return operationsContactEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
