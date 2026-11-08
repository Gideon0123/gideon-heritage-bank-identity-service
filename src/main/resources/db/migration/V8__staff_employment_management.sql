-- ============================================================
-- V7: Staff profile and employment management
-- ============================================================

-- ============================================================
-- 1. Departments
-- ============================================================

CREATE TABLE departments (

                             id UUID PRIMARY KEY,

                             department_code VARCHAR(30) NOT NULL,

                             name VARCHAR(120) NOT NULL,

                             description VARCHAR(500),

                             is_active BOOLEAN NOT NULL DEFAULT TRUE,

                             created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT uk_departments_code
                                 UNIQUE (department_code)

);

CREATE INDEX idx_departments_active
    ON departments(is_active);


-- ============================================================
-- 2. Extend staff profiles
-- ============================================================

ALTER TABLE staff_profiles

    ADD COLUMN department_id UUID,

    ADD COLUMN branch_id UUID,

    ADD COLUMN job_title VARCHAR(120),

    ADD COLUMN employment_type VARCHAR(30),

    ADD COLUMN employment_status VARCHAR(30)
        NOT NULL DEFAULT 'PENDING',

    ADD COLUMN assignment_start_date DATE,

    ADD COLUMN employment_end_date DATE,

    ADD COLUMN reporting_manager_id UUID;


-- ============================================================
-- 3. Staff profile foreign keys
-- ============================================================

ALTER TABLE staff_profiles

    ADD CONSTRAINT fk_staff_profiles_department

        FOREIGN KEY (department_id)

            REFERENCES departments(id)

            ON DELETE RESTRICT;


ALTER TABLE staff_profiles

    ADD CONSTRAINT fk_staff_profiles_reporting_manager

        FOREIGN KEY (reporting_manager_id)

            REFERENCES bank_users(id)

            ON DELETE RESTRICT;


-- ============================================================
-- 4. Staff profile indexes
-- ============================================================

CREATE INDEX idx_staff_profiles_department

    ON staff_profiles(department_id);


CREATE INDEX idx_staff_profiles_branch

    ON staff_profiles(branch_id);


CREATE INDEX idx_staff_profiles_employment_status

    ON staff_profiles(employment_status);


CREATE INDEX idx_staff_profiles_reporting_manager

    ON staff_profiles(reporting_manager_id);


-- ============================================================
-- 5. Backfill existing staff profiles
-- ============================================================

-- Existing staff have an original hire date.
-- Use that date as the beginning of their first assignment.

UPDATE staff_profiles

SET assignment_start_date = hire_date;


-- Existing staff identities that have completed onboarding
-- are considered active employees for this initial migration.
--
-- Existing staff who have not completed onboarding remain
-- in PENDING employment status.

UPDATE staff_profiles sp

SET employment_status =

        CASE

            WHEN bu.enabled = TRUE
                AND bu.email_verified = TRUE

                THEN 'ACTIVE'

            ELSE 'PENDING'

            END

    FROM bank_users bu

WHERE bu.id = sp.user_id;

ALTER TABLE staff_profiles
    ALTER COLUMN assignment_start_date SET NOT NULL;


-- ============================================================
-- 6. Employment type and status constraints
-- ============================================================

ALTER TABLE staff_profiles

    ADD CONSTRAINT chk_staff_profiles_employment_type

        CHECK (

            employment_type IS NULL

                OR employment_type IN (

                                       'FULL_TIME',

                                       'PART_TIME',

                                       'CONTRACT',

                                       'TEMPORARY',

                                       'INTERN',

                                       'PROBATIONARY'

                )

            );


ALTER TABLE staff_profiles

    ADD CONSTRAINT chk_staff_profiles_employment_status

        CHECK (

            employment_status IN (

                                  'PENDING',

                                  'ACTIVE',

                                  'ON_LEAVE',

                                  'SUSPENDED',

                                  'TERMINATED'

                )

            );


-- A staff member cannot report to themselves.

ALTER TABLE staff_profiles

    ADD CONSTRAINT chk_staff_profiles_reporting_manager

        CHECK (

            reporting_manager_id IS NULL

                OR reporting_manager_id <> user_id

            );


-- Employment end date cannot be earlier than the hire date.

ALTER TABLE staff_profiles

    ADD CONSTRAINT chk_staff_profiles_employment_dates

        CHECK (

            employment_end_date IS NULL

                OR employment_end_date >= hire_date

            );


-- A terminated employee must have an employment end date.

ALTER TABLE staff_profiles

    ADD CONSTRAINT chk_staff_profiles_termination_date

        CHECK (

            (

                employment_status = 'TERMINATED'

                    AND employment_end_date IS NOT NULL

                )

                OR

            (

                employment_status <> 'TERMINATED'

                    AND employment_end_date IS NULL

                )

            );


-- ============================================================
-- 7. Staff employment history
-- ============================================================

CREATE TABLE staff_employment_history (

                                          id UUID PRIMARY KEY,

                                          staff_profile_id UUID NOT NULL,

                                          department_id UUID,

                                          department_code_snapshot VARCHAR(30),

                                          department_name_snapshot VARCHAR(120),

                                          branch_id UUID,

                                          job_title VARCHAR(120),

                                          employment_type VARCHAR(30),

                                          employment_status VARCHAR(30) NOT NULL,

                                          effective_from DATE NOT NULL,

                                          effective_to DATE,

                                          change_reason VARCHAR(500),

                                          changed_by_user_id UUID,

                                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,


                                          CONSTRAINT fk_staff_employment_history_profile

                                              FOREIGN KEY (staff_profile_id)

                                                  REFERENCES staff_profiles(id)

                                                  ON DELETE RESTRICT,


                                          CONSTRAINT fk_staff_employment_history_department

                                              FOREIGN KEY (department_id)

                                                  REFERENCES departments(id)

                                                  ON DELETE RESTRICT,


                                          CONSTRAINT fk_staff_employment_history_changed_by

                                              FOREIGN KEY (changed_by_user_id)

                                                  REFERENCES bank_users(id)

                                                  ON DELETE RESTRICT,


                                          CONSTRAINT chk_staff_employment_history_type

                                              CHECK (

                                                  employment_type IS NULL

                                                      OR employment_type IN (

                                                                             'FULL_TIME',

                                                                             'PART_TIME',

                                                                             'CONTRACT',

                                                                             'TEMPORARY',

                                                                             'INTERN',

                                                                             'PROBATIONARY'

                                                      )

                                                  ),


                                          CONSTRAINT chk_staff_employment_history_status

                                              CHECK (

                                                  employment_status IN (

                                                                        'PENDING',

                                                                        'ACTIVE',

                                                                        'ON_LEAVE',

                                                                        'SUSPENDED',

                                                                        'TERMINATED'

                                                      )

                                                  ),


                                          CONSTRAINT chk_staff_employment_history_dates

                                              CHECK (

                                                  effective_to IS NULL

                                                      OR effective_to >= effective_from

                                                  )

);


-- ============================================================
-- 8. Employment history indexes
-- ============================================================

CREATE INDEX idx_staff_employment_history_staff

    ON staff_employment_history(staff_profile_id);


CREATE INDEX idx_staff_employment_history_department

    ON staff_employment_history(department_id);


CREATE INDEX idx_staff_employment_history_branch

    ON staff_employment_history(branch_id);


CREATE INDEX idx_staff_employment_history_effective_dates

    ON staff_employment_history(effective_from, effective_to);


CREATE INDEX idx_staff_employment_history_changed_by

    ON staff_employment_history(changed_by_user_id);


-- A staff member may have many historical assignments,
-- but only one current assignment at a time.

CREATE UNIQUE INDEX uq_staff_employment_history_current

    ON staff_employment_history(staff_profile_id)

    WHERE effective_to IS NULL;


-- ============================================================
-- 9. Backfill employment history for existing staff
-- ============================================================

INSERT INTO staff_employment_history (

    id,

    staff_profile_id,

    department_id,

    department_code_snapshot,

    department_name_snapshot,

    branch_id,

    job_title,

    employment_type,

    employment_status,

    effective_from,

    effective_to,

    change_reason,

    changed_by_user_id,

    created_at

)

SELECT

    gen_random_uuid(),

    sp.id,

    sp.department_id,

    d.department_code,

    d.name,

    sp.branch_id,

    sp.job_title,

    sp.employment_type,

    sp.employment_status,

    COALESCE(sp.assignment_start_date, sp.hire_date),

    sp.employment_end_date,

    'Initial employment history record created during migration',

    NULL,

    CURRENT_TIMESTAMP

FROM staff_profiles sp

         LEFT JOIN departments d

                   ON d.id = sp.department_id;


-- ============================================================
-- END V7
-- ============================================================