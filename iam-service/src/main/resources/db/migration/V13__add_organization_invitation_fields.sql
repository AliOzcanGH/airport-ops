ALTER TABLE iam.invitations
    ADD COLUMN invitation_type VARCHAR(30) NOT NULL DEFAULT 'PLATFORM',
    ADD COLUMN intended_role VARCHAR(30),
    ADD COLUMN invitee_full_name VARCHAR(150),
    ADD CONSTRAINT chk_invitations_invitation_type
        CHECK (invitation_type IN ('PLATFORM', 'ORGANIZATION')),
    ADD CONSTRAINT chk_invitations_intended_role
        CHECK (intended_role IS NULL OR intended_role IN ('OPS_USER', 'VIEWER')),
    ADD CONSTRAINT chk_invitations_org_type_requires_fields
        CHECK (invitation_type <> 'ORGANIZATION'
            OR (organization_id IS NOT NULL AND intended_role IS NOT NULL));
