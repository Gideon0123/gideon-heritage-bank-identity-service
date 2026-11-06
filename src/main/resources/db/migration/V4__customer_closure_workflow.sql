-- ============================================================
-- V4: Customer closure workflow and identity deactivation
-- ============================================================

-- Add terminal closure state to customer identities.
ALTER TABLE bank_users
    ADD COLUMN identity_closed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE bank_users
    ADD COLUMN closed_at TIMESTAMPTZ;

ALTER TABLE bank_users
    ADD COLUMN closure_reason VARCHAR(1000);

ALTER TABLE bank_users
    ADD CONSTRAINT chk_bank_users_identity_closure
        CHECK (
            (
                identity_closed = TRUE
                    AND enabled = FALSE
                    AND closed_at IS NOT NULL
                    AND closure_reason IS NOT NULL
                )
                OR
            (
                identity_closed = FALSE
                    AND closed_at IS NULL
                    AND closure_reason IS NULL
                )
            );

-- Closure workflow.
CREATE TABLE customer_closure_requests (
                                           id UUID PRIMARY KEY,

                                           customer_id UUID NOT NULL,

                                           status VARCHAR(40) NOT NULL,

                                           customer_reason VARCHAR(1000) NOT NULL,

                                           requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                           reviewed_by UUID,
                                           reviewed_at TIMESTAMPTZ,
                                           review_notes VARCHAR(1000),

                                           financial_clearance_reference VARCHAR(150),
                                           financial_clearance_notes VARCHAR(1000),
                                           clearance_confirmed_by UUID,
                                           financial_clearance_confirmed_at TIMESTAMPTZ,

                                           rejection_reason VARCHAR(1000),
                                           rejected_by UUID,
                                           rejected_at TIMESTAMPTZ,

                                           cancelled_at TIMESTAMPTZ,

                                           completed_by UUID,
                                           completed_at TIMESTAMPTZ,

                                           version BIGINT NOT NULL DEFAULT 0,

                                           CONSTRAINT fk_closure_customer
                                               FOREIGN KEY (customer_id)
                                                   REFERENCES bank_users(id)
                                                   ON DELETE RESTRICT,

                                           CONSTRAINT fk_closure_reviewer
                                               FOREIGN KEY (reviewed_by)
                                                   REFERENCES bank_users(id)
                                                   ON DELETE RESTRICT,

                                           CONSTRAINT fk_closure_clearance_confirmer
                                               FOREIGN KEY (clearance_confirmed_by)
                                                   REFERENCES bank_users(id)
                                                   ON DELETE RESTRICT,

                                           CONSTRAINT fk_closure_rejector
                                               FOREIGN KEY (rejected_by)
                                                   REFERENCES bank_users(id)
                                                   ON DELETE RESTRICT,

                                           CONSTRAINT fk_closure_completer
                                               FOREIGN KEY (completed_by)
                                                   REFERENCES bank_users(id)
                                                   ON DELETE RESTRICT,

                                           CONSTRAINT chk_closure_request_status
                                               CHECK (
                                                   status IN (
                                                              'REQUESTED',
                                                              'UNDER_REVIEW',
                                                              'AWAITING_FINANCIAL_CLEARANCE',
                                                              'READY_TO_CLOSE',
                                                              'REJECTED',
                                                              'CANCELLED',
                                                              'COMPLETED'
                                                       )
                                                   )
);

CREATE INDEX idx_closure_requests_customer
    ON customer_closure_requests(customer_id);

CREATE INDEX idx_closure_requests_status
    ON customer_closure_requests(status);

CREATE INDEX idx_closure_requests_requested_at
    ON customer_closure_requests(requested_at DESC);

-- A customer may have historical closure requests, but only one
-- active request at a time.
CREATE UNIQUE INDEX uq_customer_closure_one_open_request
    ON customer_closure_requests(customer_id)
    WHERE status IN (
        'REQUESTED',
        'UNDER_REVIEW',
        'AWAITING_FINANCIAL_CLEARANCE',
        'READY_TO_CLOSE'
    );