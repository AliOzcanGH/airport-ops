CREATE TABLE iam.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_users_soft_delete
        CHECK (deleted_at IS NULL OR status = 'INACTIVE')
);

CREATE UNIQUE INDEX uq_users_active_email
    ON iam.users (lower(email))
    WHERE deleted_at IS NULL;

CREATE TABLE iam.organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_organizations_status
        CHECK (status IN ('ONBOARDING_INCOMPLETE', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_organizations_soft_delete
        CHECK (deleted_at IS NULL OR status = 'INACTIVE')
);

CREATE UNIQUE INDEX uq_organizations_active_name
    ON iam.organizations (lower(name))
    WHERE deleted_at IS NULL;
