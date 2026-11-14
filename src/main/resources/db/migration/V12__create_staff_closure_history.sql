CREATE TABLE staff_closure_history (
                                       id UUID PRIMARY KEY,

                                       staff_profile_id UUID NOT NULL,
                                       user_id UUID NOT NULL,

                                       employee_number VARCHAR(50) NOT NULL,
                                       final_staff_role VARCHAR(50),

                                       closure_reason VARCHAR(1000) NOT NULL,

                                       closed_by_user_id UUID NOT NULL,
                                       closed_at TIMESTAMPTZ NOT NULL,

                                       retention_review_at TIMESTAMPTZ NOT NULL,

                                       CONSTRAINT fk_staff_closure_history_profile
                                           FOREIGN KEY (staff_profile_id)
                                               REFERENCES staff_profiles(id),

                                       CONSTRAINT fk_staff_closure_history_user
                                           FOREIGN KEY (user_id)
                                               REFERENCES bank_users(id),

                                       CONSTRAINT uq_staff_closure_history_profile
                                           UNIQUE (staff_profile_id),

                                       CONSTRAINT uq_staff_closure_history_user
                                           UNIQUE (user_id)
);

CREATE INDEX idx_staff_closure_history_closed_at
    ON staff_closure_history(closed_at);

CREATE INDEX idx_staff_closure_history_admin
    ON staff_closure_history(closed_by_user_id);

CREATE INDEX idx_staff_closure_history_retention_review
    ON staff_closure_history(retention_review_at);