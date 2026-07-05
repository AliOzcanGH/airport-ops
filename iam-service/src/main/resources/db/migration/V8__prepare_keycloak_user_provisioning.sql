ALTER TABLE iam.users
    DROP CONSTRAINT chk_users_status,
    ALTER COLUMN password_hash DROP NOT NULL,
    ADD COLUMN auth_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN keycloak_user_id VARCHAR(64);

ALTER TABLE iam.users
    ADD CONSTRAINT chk_users_status
        CHECK (status IN ('PROVISIONING', 'ACTIVE', 'KEYCLOAK_SYNC_FAILED', 'INACTIVE')),
    ADD CONSTRAINT chk_users_auth_provider
        CHECK (auth_provider IN ('LOCAL', 'KEYCLOAK'));

CREATE UNIQUE INDEX uq_users_keycloak_user_id
    ON iam.users (keycloak_user_id)
    WHERE keycloak_user_id IS NOT NULL;
