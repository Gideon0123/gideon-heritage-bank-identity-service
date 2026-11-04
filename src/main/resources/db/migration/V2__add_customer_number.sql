-- Create a sequence for customer numbers.
CREATE SEQUENCE IF NOT EXISTS customer_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Add the column as nullable temporarily so existing records can be migrated.
ALTER TABLE bank_users
    ADD COLUMN customer_number VARCHAR(12);

-- Assign customer numbers to existing customer records.
UPDATE bank_users
SET customer_number =
        'GH' || LPAD(nextval('customer_number_seq')::TEXT, 10, '0')
WHERE user_type = 'CUSTOMER'
  AND customer_number IS NULL;

-- Staff do not receive customer numbers.
-- Keep the column nullable at the database level if staff share this table.
-- Enforce uniqueness for non-null customer numbers.
ALTER TABLE bank_users
    ADD CONSTRAINT uq_bank_users_customer_number
        UNIQUE (customer_number);

-- Make sure every customer has a number.
ALTER TABLE bank_users
    ADD CONSTRAINT chk_customer_number_required
        CHECK (
            (user_type = 'CUSTOMER' AND customer_number IS NOT NULL)
                OR
            (user_type = 'STAFF' AND customer_number IS NULL)
            );