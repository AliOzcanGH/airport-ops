package com.aliozcan.airportops.iam_service.repository;

import com.aliozcan.airportops.iam_service.domain.model.OrganizationSetupProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface OrganizationSetupProfileRepository
        extends JpaRepository<OrganizationSetupProfileEntity, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO iam.organization_setup_profiles (
                organization_id,
                display_name,
                iata_code,
                icao_code,
                country_code,
                timezone,
                base_airport_iata,
                operations_contact_email,
                created_at,
                updated_at
            ) VALUES (
                :organizationId,
                :displayName,
                :iataCode,
                :icaoCode,
                :countryCode,
                :timezone,
                :baseAirportIata,
                :operationsContactEmail,
                :now,
                :now
            )
            ON CONFLICT (organization_id) DO UPDATE SET
                display_name = EXCLUDED.display_name,
                iata_code = EXCLUDED.iata_code,
                icao_code = EXCLUDED.icao_code,
                country_code = EXCLUDED.country_code,
                timezone = EXCLUDED.timezone,
                base_airport_iata = EXCLUDED.base_airport_iata,
                operations_contact_email = EXCLUDED.operations_contact_email,
                updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    int upsert(
            @Param("organizationId") UUID organizationId,
            @Param("displayName") String displayName,
            @Param("iataCode") String iataCode,
            @Param("icaoCode") String icaoCode,
            @Param("countryCode") String countryCode,
            @Param("timezone") String timezone,
            @Param("baseAirportIata") String baseAirportIata,
            @Param("operationsContactEmail") String operationsContactEmail,
            @Param("now") Instant now);
}
