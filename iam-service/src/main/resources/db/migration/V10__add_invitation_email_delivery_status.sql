ALTER TABLE iam.invitations
    ADD COLUMN email_delivery_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SENT',
    ADD COLUMN email_sent_at TIMESTAMPTZ,
    ADD COLUMN email_failure_reason VARCHAR(500),
    ADD CONSTRAINT chk_invitations_email_delivery_status
        CHECK (email_delivery_status IN ('NOT_SENT', 'SENT', 'FAILED'));
