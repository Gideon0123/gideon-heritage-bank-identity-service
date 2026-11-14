CREATE TABLE customer_staff_conversions (
                                            id UUID PRIMARY KEY,

                                            user_id UUID NOT NULL,

                                            customer_number VARCHAR(12) NOT NULL,

                                            employee_number VARCHAR(50) NOT NULL,

                                            assigned_staff_role VARCHAR(50) NOT NULL,

                                            conversion_reason VARCHAR(1000) NOT NULL,

                                            converted_by_user_id UUID NOT NULL,

                                            converted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                            CONSTRAINT fk_customer_staff_conversions_user
                                                FOREIGN KEY (user_id)
                                                    REFERENCES bank_users(id),

                                            CONSTRAINT uk_customer_staff_conversions_user
                                                UNIQUE (user_id),

                                            CONSTRAINT uk_customer_staff_conversions_employee_number
                                                UNIQUE (employee_number)
);

CREATE INDEX idx_customer_staff_conversions_converted_at
    ON customer_staff_conversions(converted_at);

CREATE INDEX idx_customer_staff_conversions_admin
    ON customer_staff_conversions(converted_by_user_id);