ALTER TABLE iam.users
    ADD COLUMN preferred_language VARCHAR(2) NOT NULL DEFAULT 'EN',
    ADD CONSTRAINT chk_users_preferred_language
        CHECK (preferred_language IN ('TR', 'EN'));
