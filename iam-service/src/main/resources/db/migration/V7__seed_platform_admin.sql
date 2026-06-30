UPDATE iam.users
SET email = 'platform.admin@demo.com',
    full_name = 'Platform Admin',
    status = 'ACTIVE',
    password_hash = '$2y$10$wQmzRWJ7omqLDAEjIRzvpejjJMyarzcdn79Y/U1b0QOyud4C1R9CG',
    updated_at = now()
WHERE lower(email) = 'platform.admin@demo.com'
  AND deleted_at IS NULL;

INSERT INTO iam.users (email, password_hash, full_name, status)
SELECT
    'platform.admin@demo.com',
    '$2y$10$wQmzRWJ7omqLDAEjIRzvpejjJMyarzcdn79Y/U1b0QOyud4C1R9CG',
    'Platform Admin',
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.users
    WHERE lower(email) = 'platform.admin@demo.com'
      AND deleted_at IS NULL
);

INSERT INTO iam.platform_user_roles (user_id, role_id)
SELECT user_record.id, role_record.id
FROM iam.users user_record
JOIN iam.roles role_record
    ON role_record.code = 'PLATFORM_ADMIN'
   AND role_record.scope = 'PLATFORM'
WHERE lower(user_record.email) = 'platform.admin@demo.com'
  AND user_record.status = 'ACTIVE'
  AND user_record.deleted_at IS NULL
ON CONFLICT (user_id, role_id) DO NOTHING;

DO $$
DECLARE
    canonical_user_id UUID;
    active_user_count INTEGER;
    platform_admin_role_id UUID;
    role_count INTEGER;
    mapping_count INTEGER;
    organization_membership_count INTEGER;
BEGIN
    SELECT count(*)
    INTO active_user_count
    FROM iam.users
    WHERE lower(email) = 'platform.admin@demo.com'
      AND email = 'platform.admin@demo.com'
      AND full_name = 'Platform Admin'
      AND status = 'ACTIVE'
      AND password_hash = '$2y$10$wQmzRWJ7omqLDAEjIRzvpejjJMyarzcdn79Y/U1b0QOyud4C1R9CG'
      AND deleted_at IS NULL;

    IF active_user_count <> 1 THEN
        RAISE EXCEPTION
            'Platform admin seed validation failed: expected 1 canonical active user, found %',
            active_user_count;
    END IF;

    SELECT id
    INTO canonical_user_id
    FROM iam.users
    WHERE lower(email) = 'platform.admin@demo.com'
      AND email = 'platform.admin@demo.com'
      AND full_name = 'Platform Admin'
      AND status = 'ACTIVE'
      AND password_hash = '$2y$10$wQmzRWJ7omqLDAEjIRzvpejjJMyarzcdn79Y/U1b0QOyud4C1R9CG'
      AND deleted_at IS NULL;

    SELECT count(*)
    INTO role_count
    FROM iam.roles
    WHERE code = 'PLATFORM_ADMIN'
      AND scope = 'PLATFORM';

    IF role_count <> 1 THEN
        RAISE EXCEPTION
            'Platform admin role validation failed: expected 1 PLATFORM_ADMIN role with PLATFORM scope, found %',
            role_count;
    END IF;

    SELECT id
    INTO platform_admin_role_id
    FROM iam.roles
    WHERE code = 'PLATFORM_ADMIN'
      AND scope = 'PLATFORM';

    SELECT count(*)
    INTO mapping_count
    FROM iam.platform_user_roles
    WHERE user_id = canonical_user_id
      AND role_id = platform_admin_role_id;

    IF mapping_count <> 1 THEN
        RAISE EXCEPTION
            'Platform admin mapping validation failed: expected 1 user-role mapping, found %',
            mapping_count;
    END IF;

    SELECT count(*)
    INTO organization_membership_count
    FROM iam.organization_members
    WHERE user_id = canonical_user_id;

    IF organization_membership_count <> 0 THEN
        RAISE EXCEPTION
            'Platform admin membership validation failed: expected 0 organization memberships, found %',
            organization_membership_count;
    END IF;
END
$$;
