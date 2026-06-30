CREATE TABLE iam.roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    scope VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_roles_code UNIQUE (code),
    CONSTRAINT chk_roles_scope
        CHECK (scope IN ('PLATFORM', 'ORGANIZATION'))
);

CREATE TABLE iam.permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(120) NOT NULL,
    description VARCHAR(255) NOT NULL,
    scope VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_permissions_code UNIQUE (code),
    CONSTRAINT chk_permissions_scope
        CHECK (scope IN ('PLATFORM', 'ORGANIZATION'))
);

CREATE TABLE iam.role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT pk_role_permissions
        PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES iam.roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES iam.permissions (id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_permission_id
    ON iam.role_permissions (permission_id);
