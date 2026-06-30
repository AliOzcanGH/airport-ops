CREATE TABLE iam.organization_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    joined_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_organization_members_organization
        FOREIGN KEY (organization_id) REFERENCES iam.organizations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_organization_members_user
        FOREIGN KEY (user_id) REFERENCES iam.users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_organization_members_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'INVITED')),
    CONSTRAINT chk_organization_members_soft_delete
        CHECK (deleted_at IS NULL OR status = 'INACTIVE')
);

CREATE UNIQUE INDEX uq_organization_members_active_membership
    ON iam.organization_members (organization_id, user_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_organization_members_active_user
    ON iam.organization_members (user_id)
    WHERE deleted_at IS NULL;

CREATE TABLE iam.platform_user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_platform_user_roles
        PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_platform_user_roles_user
        FOREIGN KEY (user_id) REFERENCES iam.users (id) ON DELETE CASCADE,
    CONSTRAINT fk_platform_user_roles_role
        FOREIGN KEY (role_id) REFERENCES iam.roles (id) ON DELETE CASCADE
);

CREATE INDEX idx_platform_user_roles_role_id
    ON iam.platform_user_roles (role_id);

CREATE TABLE iam.member_roles (
    member_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_member_roles
        PRIMARY KEY (member_id, role_id),
    CONSTRAINT fk_member_roles_member
        FOREIGN KEY (member_id) REFERENCES iam.organization_members (id) ON DELETE CASCADE,
    CONSTRAINT fk_member_roles_role
        FOREIGN KEY (role_id) REFERENCES iam.roles (id) ON DELETE CASCADE
);

CREATE INDEX idx_member_roles_role_id
    ON iam.member_roles (role_id);
