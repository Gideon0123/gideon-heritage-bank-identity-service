
-- ============================================================
-- V13: Create Branch Management
-- ============================================================


-- ============================================================
-- 1. Create branches
-- ============================================================

CREATE TABLE branches (

    id UUID PRIMARY KEY,

    branch_code VARCHAR(50) NOT NULL,
    name VARCHAR(120) NOT NULL,

    branch_type VARCHAR(30) NOT NULL,

    address_line1 VARCHAR(180) NOT NULL,
    address_line2 VARCHAR(180),

    city VARCHAR(100) NOT NULL,
    state_or_region VARCHAR(100) NOT NULL,

    postal_code VARCHAR(20),

    country_code CHAR(2) NOT NULL DEFAULT 'NG',

    phone_number VARCHAR(16),
    email VARCHAR(254),

    branch_manager_id UUID,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    opened_date DATE NOT NULL,

    closed_at TIMESTAMPTZ,
    closure_reason VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_branches_branch_code
        UNIQUE (branch_code),

    CONSTRAINT uk_branches_branch_manager
        UNIQUE (branch_manager_id),

    CONSTRAINT fk_branches_branch_manager
        FOREIGN KEY (branch_manager_id)
            REFERENCES bank_users(id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_branches_type
        CHECK (
            branch_type IN (
                'HEAD_OFFICE',
                'REGIONAL',
                'STANDARD',
                'EXPRESS'
            )
        ),

    CONSTRAINT chk_branches_status
        CHECK (
            status IN (
                'ACTIVE',
                'INACTIVE',
                'CLOSED'
            )
        ),

    CONSTRAINT chk_branches_country_code
        CHECK (
            country_code ~ '^[A-Z]{2}$'
        ),

    CONSTRAINT chk_branches_closure
        CHECK (
            (
                status = 'CLOSED'
                AND closed_at IS NOT NULL
                AND closure_reason IS NOT NULL
            )
            OR
            (
                status <> 'CLOSED'
                AND closed_at IS NULL
                AND closure_reason IS NULL
            )
        )

);


-- ============================================================
-- 2. Branch indexes
-- ============================================================

CREATE INDEX idx_branches_status
    ON branches(status);

CREATE INDEX idx_branches_city
    ON branches(city);

CREATE INDEX idx_branches_state
    ON branches(state_or_region);

CREATE INDEX idx_branches_branch_manager
    ON branches(branch_manager_id);


-- ============================================================
-- 3. Preserve existing legacy branch references
-- ============================================================

-- Earlier versions of the Identity Service allowed branch_id
-- values without a corresponding Branch entity.
--
-- Preserve those UUIDs by creating inactive placeholder records.
-- Administrators should update these records with actual branch
-- information after Branch management is implemented.

INSERT INTO branches (
    id,
    branch_code,
    name,
    branch_type,
    address_line1,
    city,
    state_or_region,
    country_code,
    status,
    opened_date,
    created_at,
    updated_at
)

SELECT
    existing_branch.id,

    'LEGACY-' || REPLACE(existing_branch.id::TEXT, '-', ''),

    'Legacy branch - ' || existing_branch.id::TEXT,

    'STANDARD',

    'ADDRESS NOT PROVIDED - UPDATE REQUIRED',

    'UNKNOWN',

    'UNKNOWN',

    'NG',

    'INACTIVE',

    CURRENT_DATE,

    CURRENT_TIMESTAMP,

    CURRENT_TIMESTAMP

FROM (

    SELECT branch_id AS id
    FROM staff_profiles
    WHERE branch_id IS NOT NULL

    UNION

    SELECT branch_id AS id
    FROM staff_employment_history
    WHERE branch_id IS NOT NULL

) existing_branch

ON CONFLICT (id) DO NOTHING;


-- ============================================================
-- 4. Add Branch foreign key to staff_profiles
-- ============================================================

ALTER TABLE staff_profiles

    ADD CONSTRAINT fk_staff_profiles_branch

        FOREIGN KEY (branch_id)

            REFERENCES branches(id)

            ON DELETE RESTRICT;


-- ============================================================
-- 5. Extend staff employment history
-- ============================================================

ALTER TABLE staff_employment_history

    ADD COLUMN branch_code_snapshot VARCHAR(50),

    ADD COLUMN branch_name_snapshot VARCHAR(120);


-- ============================================================
-- 6. Backfill historical branch snapshots
-- ============================================================

UPDATE staff_employment_history history

SET
    branch_code_snapshot = branch.branch_code,

    branch_name_snapshot = branch.name

FROM branches branch

WHERE branch.id = history.branch_id;


-- ============================================================
-- 7. Add Branch foreign key to employment history
-- ============================================================

ALTER TABLE staff_employment_history

    ADD CONSTRAINT fk_staff_employment_history_branch

        FOREIGN KEY (branch_id)

            REFERENCES branches(id)

            ON DELETE RESTRICT;


-- ============================================================
-- END V13
-- ============================================================
