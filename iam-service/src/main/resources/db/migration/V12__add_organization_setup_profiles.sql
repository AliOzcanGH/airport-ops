CREATE TABLE iam.organization_setup_profiles (
    organization_id UUID PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    iata_code VARCHAR(2),
    icao_code VARCHAR(3),
    country_code VARCHAR(2),
    timezone VARCHAR(80),
    base_airport_iata VARCHAR(3),
    operations_contact_email VARCHAR(254),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_organization_setup_profiles_organization
        FOREIGN KEY (organization_id) REFERENCES iam.organizations (id) ON DELETE CASCADE,
    CONSTRAINT chk_organization_setup_profiles_display_name
        CHECK (btrim(display_name) <> ''),
    CONSTRAINT chk_organization_setup_profiles_iata_code
        CHECK (iata_code IS NULL OR iata_code ~ '^[A-Z0-9]{2}$'),
    CONSTRAINT chk_organization_setup_profiles_icao_code
        CHECK (icao_code IS NULL OR icao_code ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_organization_setup_profiles_country_code
        CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT chk_organization_setup_profiles_base_airport_iata
        CHECK (base_airport_iata IS NULL OR base_airport_iata ~ '^[A-Z]{3}$')
);
