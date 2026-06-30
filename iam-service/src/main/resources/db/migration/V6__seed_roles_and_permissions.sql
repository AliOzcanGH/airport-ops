INSERT INTO iam.roles (code, name, scope)
VALUES
    ('PLATFORM_ADMIN', 'Platform Admin', 'PLATFORM'),
    ('AIRLINE_ADMIN', 'Airline Admin', 'ORGANIZATION'),
    ('OPS_USER', 'Operations User', 'ORGANIZATION'),
    ('VIEWER', 'Viewer', 'ORGANIZATION')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    scope = EXCLUDED.scope;

INSERT INTO iam.permissions (code, description, scope)
VALUES
    ('platform:invitation:create', 'Allows creating platform invitations.', 'PLATFORM'),
    ('tenant:read', 'Allows reading tenant information.', 'PLATFORM'),
    ('tenant:manage', 'Allows managing tenants.', 'PLATFORM'),
    ('member:invite', 'Allows inviting organization members.', 'ORGANIZATION'),
    ('member:role:update', 'Allows updating organization member roles.', 'ORGANIZATION'),
    ('member:read', 'Allows reading organization members.', 'ORGANIZATION'),
    ('member:remove', 'Allows removing organization members.', 'ORGANIZATION'),
    ('station:create', 'Allows creating stations.', 'ORGANIZATION'),
    ('station:read', 'Allows reading stations.', 'ORGANIZATION'),
    ('station:update', 'Allows updating stations.', 'ORGANIZATION'),
    ('gate:read', 'Allows reading gates.', 'ORGANIZATION'),
    ('gate:update', 'Allows updating gates.', 'ORGANIZATION'),
    ('flight:create', 'Allows creating flights.', 'ORGANIZATION'),
    ('flight:read', 'Allows reading flights.', 'ORGANIZATION'),
    ('flight:update', 'Allows updating flights.', 'ORGANIZATION'),
    ('flight:cancel', 'Allows cancelling flights.', 'ORGANIZATION'),
    ('task:read', 'Allows reading tasks.', 'ORGANIZATION'),
    ('task:complete', 'Allows completing tasks.', 'ORGANIZATION'),
    ('report:read', 'Allows reading reports.', 'ORGANIZATION'),
    ('audit:read', 'Allows reading audit records.', 'ORGANIZATION')
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    scope = EXCLUDED.scope;

WITH expected_mappings (role_code, permission_code) AS (
    VALUES
        ('PLATFORM_ADMIN', 'platform:invitation:create'),
        ('PLATFORM_ADMIN', 'tenant:read'),
        ('PLATFORM_ADMIN', 'tenant:manage'),
        ('AIRLINE_ADMIN', 'member:invite'),
        ('AIRLINE_ADMIN', 'member:role:update'),
        ('AIRLINE_ADMIN', 'member:read'),
        ('AIRLINE_ADMIN', 'member:remove'),
        ('AIRLINE_ADMIN', 'station:create'),
        ('AIRLINE_ADMIN', 'station:read'),
        ('AIRLINE_ADMIN', 'station:update'),
        ('AIRLINE_ADMIN', 'gate:read'),
        ('AIRLINE_ADMIN', 'gate:update'),
        ('AIRLINE_ADMIN', 'flight:create'),
        ('AIRLINE_ADMIN', 'flight:read'),
        ('AIRLINE_ADMIN', 'flight:update'),
        ('AIRLINE_ADMIN', 'flight:cancel'),
        ('AIRLINE_ADMIN', 'task:read'),
        ('AIRLINE_ADMIN', 'task:complete'),
        ('AIRLINE_ADMIN', 'report:read'),
        ('AIRLINE_ADMIN', 'audit:read'),
        ('OPS_USER', 'station:read'),
        ('OPS_USER', 'gate:read'),
        ('OPS_USER', 'flight:create'),
        ('OPS_USER', 'flight:read'),
        ('OPS_USER', 'flight:update'),
        ('OPS_USER', 'task:read'),
        ('OPS_USER', 'task:complete'),
        ('OPS_USER', 'report:read'),
        ('VIEWER', 'station:read'),
        ('VIEWER', 'gate:read'),
        ('VIEWER', 'flight:read'),
        ('VIEWER', 'task:read'),
        ('VIEWER', 'report:read')
)
INSERT INTO iam.role_permissions (role_id, permission_id)
SELECT role_record.id, permission_record.id
FROM expected_mappings mapping
JOIN iam.roles role_record
    ON role_record.code = mapping.role_code
JOIN iam.permissions permission_record
    ON permission_record.code = mapping.permission_code
   AND role_record.scope = permission_record.scope
ON CONFLICT (role_id, permission_id) DO NOTHING;

DO $$
DECLARE
    canonical_role_count INTEGER;
    canonical_permission_count INTEGER;
    expected_mapping_count INTEGER;
    scope_mismatch_count INTEGER;
BEGIN
    WITH canonical_roles (code, name, scope) AS (
        VALUES
            ('PLATFORM_ADMIN', 'Platform Admin', 'PLATFORM'),
            ('AIRLINE_ADMIN', 'Airline Admin', 'ORGANIZATION'),
            ('OPS_USER', 'Operations User', 'ORGANIZATION'),
            ('VIEWER', 'Viewer', 'ORGANIZATION')
    )
    SELECT count(*)
    INTO canonical_role_count
    FROM canonical_roles canonical
    JOIN iam.roles role_record
        ON role_record.code = canonical.code
       AND role_record.name = canonical.name
       AND role_record.scope = canonical.scope;

    IF canonical_role_count <> 4 THEN
        RAISE EXCEPTION
            'IAM role seed validation failed: expected 4 canonical roles, found %',
            canonical_role_count;
    END IF;

    WITH canonical_permissions (code, description, scope) AS (
        VALUES
            ('platform:invitation:create', 'Allows creating platform invitations.', 'PLATFORM'),
            ('tenant:read', 'Allows reading tenant information.', 'PLATFORM'),
            ('tenant:manage', 'Allows managing tenants.', 'PLATFORM'),
            ('member:invite', 'Allows inviting organization members.', 'ORGANIZATION'),
            ('member:role:update', 'Allows updating organization member roles.', 'ORGANIZATION'),
            ('member:read', 'Allows reading organization members.', 'ORGANIZATION'),
            ('member:remove', 'Allows removing organization members.', 'ORGANIZATION'),
            ('station:create', 'Allows creating stations.', 'ORGANIZATION'),
            ('station:read', 'Allows reading stations.', 'ORGANIZATION'),
            ('station:update', 'Allows updating stations.', 'ORGANIZATION'),
            ('gate:read', 'Allows reading gates.', 'ORGANIZATION'),
            ('gate:update', 'Allows updating gates.', 'ORGANIZATION'),
            ('flight:create', 'Allows creating flights.', 'ORGANIZATION'),
            ('flight:read', 'Allows reading flights.', 'ORGANIZATION'),
            ('flight:update', 'Allows updating flights.', 'ORGANIZATION'),
            ('flight:cancel', 'Allows cancelling flights.', 'ORGANIZATION'),
            ('task:read', 'Allows reading tasks.', 'ORGANIZATION'),
            ('task:complete', 'Allows completing tasks.', 'ORGANIZATION'),
            ('report:read', 'Allows reading reports.', 'ORGANIZATION'),
            ('audit:read', 'Allows reading audit records.', 'ORGANIZATION')
    )
    SELECT count(*)
    INTO canonical_permission_count
    FROM canonical_permissions canonical
    JOIN iam.permissions permission_record
        ON permission_record.code = canonical.code
       AND permission_record.description = canonical.description
       AND permission_record.scope = canonical.scope;

    IF canonical_permission_count <> 20 THEN
        RAISE EXCEPTION
            'IAM permission seed validation failed: expected 20 canonical permissions, found %',
            canonical_permission_count;
    END IF;

    WITH expected_mappings (role_code, permission_code) AS (
        VALUES
            ('PLATFORM_ADMIN', 'platform:invitation:create'),
            ('PLATFORM_ADMIN', 'tenant:read'),
            ('PLATFORM_ADMIN', 'tenant:manage'),
            ('AIRLINE_ADMIN', 'member:invite'),
            ('AIRLINE_ADMIN', 'member:role:update'),
            ('AIRLINE_ADMIN', 'member:read'),
            ('AIRLINE_ADMIN', 'member:remove'),
            ('AIRLINE_ADMIN', 'station:create'),
            ('AIRLINE_ADMIN', 'station:read'),
            ('AIRLINE_ADMIN', 'station:update'),
            ('AIRLINE_ADMIN', 'gate:read'),
            ('AIRLINE_ADMIN', 'gate:update'),
            ('AIRLINE_ADMIN', 'flight:create'),
            ('AIRLINE_ADMIN', 'flight:read'),
            ('AIRLINE_ADMIN', 'flight:update'),
            ('AIRLINE_ADMIN', 'flight:cancel'),
            ('AIRLINE_ADMIN', 'task:read'),
            ('AIRLINE_ADMIN', 'task:complete'),
            ('AIRLINE_ADMIN', 'report:read'),
            ('AIRLINE_ADMIN', 'audit:read'),
            ('OPS_USER', 'station:read'),
            ('OPS_USER', 'gate:read'),
            ('OPS_USER', 'flight:create'),
            ('OPS_USER', 'flight:read'),
            ('OPS_USER', 'flight:update'),
            ('OPS_USER', 'task:read'),
            ('OPS_USER', 'task:complete'),
            ('OPS_USER', 'report:read'),
            ('VIEWER', 'station:read'),
            ('VIEWER', 'gate:read'),
            ('VIEWER', 'flight:read'),
            ('VIEWER', 'task:read'),
            ('VIEWER', 'report:read')
    )
    SELECT count(*)
    INTO expected_mapping_count
    FROM expected_mappings mapping
    JOIN iam.roles role_record
        ON role_record.code = mapping.role_code
    JOIN iam.permissions permission_record
        ON permission_record.code = mapping.permission_code
    JOIN iam.role_permissions role_permission
        ON role_permission.role_id = role_record.id
       AND role_permission.permission_id = permission_record.id;

    IF expected_mapping_count <> 33 THEN
        RAISE EXCEPTION
            'IAM role-permission seed validation failed: expected 33 mappings, found %',
            expected_mapping_count;
    END IF;

    SELECT count(*)
    INTO scope_mismatch_count
    FROM iam.role_permissions role_permission
    JOIN iam.roles role_record
        ON role_record.id = role_permission.role_id
    JOIN iam.permissions permission_record
        ON permission_record.id = role_permission.permission_id
    WHERE role_record.scope <> permission_record.scope;

    IF scope_mismatch_count <> 0 THEN
        RAISE EXCEPTION
            'IAM role-permission scope validation failed: found % mismatched mappings',
            scope_mismatch_count;
    END IF;
END
$$;
