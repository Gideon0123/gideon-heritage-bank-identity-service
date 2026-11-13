CREATE TABLE staff_role_history
(
    id UUID PRIMARY KEY,

    staff_profile_id UUID NOT NULL,

    previous_role VARCHAR(50),

    new_role VARCHAR(50),

    change_type VARCHAR(30) NOT NULL,

    change_reason VARCHAR(1000) NOT NULL,

    changed_by_user_id UUID NOT NULL,

    changed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_staff_role_history_staff
        FOREIGN KEY (staff_profile_id)
            REFERENCES staff_profiles(id)
);

CREATE INDEX idx_staff_role_history_profile
    ON staff_role_history(staff_profile_id);

CREATE INDEX idx_staff_role_history_changed_at
    ON staff_role_history(changed_at);

CREATE INDEX idx_staff_role_history_changed_by
    ON staff_role_history(changed_by_user_id);

ALTER TABLE staff_profiles
    ALTER COLUMN staff_role DROP NOT NULL;