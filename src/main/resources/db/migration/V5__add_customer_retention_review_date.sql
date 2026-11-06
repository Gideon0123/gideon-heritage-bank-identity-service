ALTER TABLE bank_users
    ADD COLUMN retention_review_at TIMESTAMPTZ;

CREATE INDEX idx_bank_users_retention_review_at
    ON bank_users(retention_review_at)
    WHERE identity_closed = TRUE;