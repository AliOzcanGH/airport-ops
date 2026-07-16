CREATE TABLE iam.user_totp_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    secret_ciphertext TEXT NOT NULL,
    secret_nonce VARCHAR(64) NOT NULL,
    secret_key_version VARCHAR(30) NOT NULL DEFAULT 'v1',
    verified_at TIMESTAMPTZ,
    disabled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_totp_credentials_user_id UNIQUE (user_id),
    CONSTRAINT chk_user_totp_credentials_status
        CHECK (status IN ('ENABLED', 'DISABLED')),
    CONSTRAINT fk_user_totp_credentials_user
        FOREIGN KEY (user_id) REFERENCES iam.users (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_totp_credentials_user_id_status
    ON iam.user_totp_credentials (user_id, status);

CREATE TABLE iam.mfa_login_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    access_token_ciphertext TEXT NOT NULL,
    access_token_nonce VARCHAR(64) NOT NULL,
    refresh_token_ciphertext TEXT NOT NULL,
    refresh_token_nonce VARCHAR(64) NOT NULL,
    token_obtained_at TIMESTAMPTZ NOT NULL,
    access_expires_in INTEGER NOT NULL,
    refresh_expires_in INTEGER NOT NULL,
    totp_secret_ciphertext TEXT,
    totp_secret_nonce VARCHAR(64),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_mfa_login_challenges_type
        CHECK (type IN ('VERIFY', 'ENROLL')),
    CONSTRAINT chk_mfa_login_challenges_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'EXPIRED', 'LOCKED')),
    CONSTRAINT fk_mfa_login_challenges_user
        FOREIGN KEY (user_id) REFERENCES iam.users (id) ON DELETE CASCADE
);

CREATE INDEX idx_mfa_login_challenges_user_id_status
    ON iam.mfa_login_challenges (user_id, status);

CREATE INDEX idx_mfa_login_challenges_status_expires_at
    ON iam.mfa_login_challenges (status, expires_at);

CREATE INDEX idx_mfa_login_challenges_expires_at
    ON iam.mfa_login_challenges (expires_at);
