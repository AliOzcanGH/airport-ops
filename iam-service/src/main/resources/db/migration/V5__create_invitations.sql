CREATE TABLE iam.invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name VARCHAR(200) NOT NULL,
    admin_email VARCHAR(320) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by_user_id UUID NOT NULL,
    organization_id UUID,
    station_setup_payload JSONB,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_invitations_token_hash UNIQUE (token_hash),
    CONSTRAINT chk_invitations_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT fk_invitations_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES iam.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_invitations_organization
        FOREIGN KEY (organization_id) REFERENCES iam.organizations (id) ON DELETE SET NULL
);

CREATE INDEX idx_invitations_admin_email
    ON iam.invitations (lower(admin_email));

CREATE UNIQUE INDEX uq_invitations_pending_admin_email
    ON iam.invitations (lower(admin_email))
    WHERE status = 'PENDING';

CREATE INDEX idx_invitations_created_by_user_id
    ON iam.invitations (created_by_user_id);

CREATE INDEX idx_invitations_organization_id
    ON iam.invitations (organization_id);
