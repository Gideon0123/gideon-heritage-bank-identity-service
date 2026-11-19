CREATE TABLE identity_outbox_events
(
    id                UUID         NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    exchange_name     VARCHAR(255) NOT NULL,
    routing_key       VARCHAR(255) NOT NULL,
    message_id        VARCHAR(255) NOT NULL,
    encrypted_payload TEXT         NOT NULL,

    status            VARCHAR(20)  NOT NULL,
    attempts          INTEGER      NOT NULL DEFAULT 0,

    available_at      TIMESTAMPTZ  NOT NULL,
    locked_until      TIMESTAMPTZ,
    lock_owner        VARCHAR(100),

    last_error        VARCHAR(500),

    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at      TIMESTAMPTZ,

    version           BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_identity_outbox_events
        PRIMARY KEY (id),

    CONSTRAINT uq_identity_outbox_event_message_id
        UNIQUE (event_type, message_id),

    CONSTRAINT chk_identity_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),

    CONSTRAINT chk_identity_outbox_attempts
        CHECK (attempts >= 0)
);

CREATE INDEX idx_identity_outbox_pending
    ON identity_outbox_events (status, available_at, created_at);

CREATE INDEX idx_identity_outbox_processing
    ON identity_outbox_events (status, locked_until);

COMMENT ON TABLE identity_outbox_events IS
    'Transactional producer outbox. Payloads are AES-256-GCM encrypted because they contain one-time credential tokens.';

COMMENT ON COLUMN identity_outbox_events.encrypted_payload IS
    'Base64 encoded nonce plus AES-GCM ciphertext. Never store or log plaintext event payloads here.';