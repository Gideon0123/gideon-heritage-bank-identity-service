-- Allow a converted staff identity to retain its original customer number.
-- Customer identities must still have a customer number.

ALTER TABLE bank_users
DROP CONSTRAINT IF EXISTS chk_customer_number_required;

ALTER TABLE bank_users
    ADD CONSTRAINT chk_customer_number_required
        CHECK (
            (user_type = 'CUSTOMER' AND customer_number IS NOT NULL)
                OR
            (user_type = 'STAFF')
            );