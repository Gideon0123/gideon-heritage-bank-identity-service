-- ============================================================
-- V16: Email change workflow
-- ============================================================

-- ------------------------------------------------------------
-- 1. Email change lifecycle fields
-- ------------------------------------------------------------

ALTER TABLE bank_users
    ADD COLUMN last_email_change_requested_at TIMESTAMPTZ,
    ADD COLUMN email_changed_at TIMESTAMPTZ,
    ADD COLUMN email_change_locked_until TIMESTAMPTZ;


-- ------------------------------------------------------------
-- 2. Email change verification tokens
-- ------------------------------------------------------------

CREATE TABLE email_change_tokens
(
    id          UUID         NOT NULL,
    user_id     UUID         NOT NULL,

    old_email   VARCHAR(254) NOT NULL,
    new_email   VARCHAR(254) NOT NULL,

    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,

    used_at     TIMESTAMPTZ,
    revoked_at  TIMESTAMPTZ,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_email_change_tokens
        PRIMARY KEY (id),

    CONSTRAINT uq_email_change_tokens_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_email_change_tokens_user
        FOREIGN KEY (user_id)
            REFERENCES bank_users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_email_change_tokens_user
    ON email_change_tokens (user_id);

CREATE INDEX idx_email_change_tokens_expires
    ON email_change_tokens (expires_at);

CREATE INDEX idx_bank_users_email_change_locked_until
    ON bank_users (email_change_locked_until);

COMMENT ON TABLE email_change_tokens IS
    'Email ownership verification tokens for email changes requested by customers and staff.';

COMMENT ON COLUMN email_change_tokens.token_hash IS
    'SHA-256 hash of the one-time email change verification token. Raw tokens are never stored.';