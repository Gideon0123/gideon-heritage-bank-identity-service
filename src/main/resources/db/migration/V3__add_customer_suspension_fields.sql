ALTER TABLE bank_users
    ADD COLUMN suspended BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE bank_users
    ADD COLUMN suspension_reason VARCHAR(1000);

ALTER TABLE bank_users
    ADD COLUMN suspended_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE bank_users
    ADD CONSTRAINT chk_bank_users_suspension_data
        CHECK (
            (
                suspended = TRUE
                    AND suspension_reason IS NOT NULL
                    AND suspended_at IS NOT NULL
                )
                OR
            (
                suspended = FALSE
                    AND suspension_reason IS NULL
                    AND suspended_at IS NULL
                )
            );