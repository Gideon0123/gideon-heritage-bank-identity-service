-- ============================================================
-- Gideon Heritage Bank PLC
-- Identity Service - Initial database schema
-- Flyway migration: V1
-- ============================================================

-- ------------------------------------------------------------
-- 1. Application roles
-- ------------------------------------------------------------

CREATE TABLE roles (
                       id UUID PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE,
                       description VARCHAR(255),
                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 2. Customer and staff identities
-- ------------------------------------------------------------

CREATE TABLE bank_users (
                            id UUID PRIMARY KEY,

                            first_name VARCHAR(100) NOT NULL,
                            last_name VARCHAR(100) NOT NULL,

                            email VARCHAR(254) NOT NULL,
                            phone_number VARCHAR(30) NOT NULL,

                            password_hash VARCHAR(255) NOT NULL,

                            user_type VARCHAR(20) NOT NULL,

                            enabled BOOLEAN NOT NULL DEFAULT FALSE,
                            email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                            account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,

                            failed_login_attempts INTEGER NOT NULL DEFAULT 0,
                            locked_until TIMESTAMPTZ,

                            profile_photo_url VARCHAR(1000),

                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT uq_bank_users_email UNIQUE (email),
                            CONSTRAINT uq_bank_users_phone UNIQUE (phone_number),

                            CONSTRAINT chk_bank_users_type
                                CHECK (user_type IN ('CUSTOMER', 'STAFF')),

                            CONSTRAINT chk_failed_login_attempts
                                CHECK (failed_login_attempts >= 0)
);

CREATE INDEX idx_bank_users_last_name
    ON bank_users (last_name);

CREATE INDEX idx_bank_users_created_at
    ON bank_users (created_at);

-- ------------------------------------------------------------
-- 3. User-to-role assignments
-- ------------------------------------------------------------

CREATE TABLE user_roles (
                            user_id UUID NOT NULL,
                            role_id UUID NOT NULL,

                            PRIMARY KEY (user_id, role_id),

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES bank_users (id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_user_roles_role
                                FOREIGN KEY (role_id)
                                    REFERENCES roles (id)
                                    ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_role_id
    ON user_roles (role_id);

-- ------------------------------------------------------------
-- 4. User addresses
-- ------------------------------------------------------------

CREATE TABLE user_addresses (
                                id UUID PRIMARY KEY,

                                user_id UUID NOT NULL,

                                address_line_1 VARCHAR(255) NOT NULL,
                                address_line_2 VARCHAR(255),

                                city VARCHAR(100) NOT NULL,
                                state_or_region VARCHAR(100),
                                postal_code VARCHAR(30),
                                country_code CHAR(2) NOT NULL,

                                address_type VARCHAR(30) NOT NULL DEFAULT 'RESIDENTIAL',
                                is_primary BOOLEAN NOT NULL DEFAULT TRUE,

                                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_user_addresses_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES bank_users (id)
                                        ON DELETE CASCADE,

                                CONSTRAINT chk_user_address_type
                                    CHECK (
                                        address_type IN (
                                                         'RESIDENTIAL',
                                                         'MAILING',
                                                         'BUSINESS'
                                            )
                                        )
);

CREATE INDEX idx_user_addresses_user_id
    ON user_addresses (user_id);

-- ------------------------------------------------------------
-- 5. Staff-specific information
-- ------------------------------------------------------------

CREATE TABLE staff_profiles (
                                id UUID PRIMARY KEY,

                                user_id UUID NOT NULL UNIQUE,
                                employee_number VARCHAR(50) NOT NULL UNIQUE,

                                staff_role VARCHAR(50) NOT NULL,
                                hire_date DATE NOT NULL,
                                last_login_at TIMESTAMPTZ,

                                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_staff_profiles_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES bank_users (id)
                                        ON DELETE RESTRICT,

                                CONSTRAINT chk_staff_role
                                    CHECK (
                                        staff_role IN (
                                                       'CASHIER',
                                                       'MANAGER',
                                                       'BRANCH_MANAGER',
                                                       'ADMIN',
                                                       'RISK_OFFICER',
                                                       'SUPPORT_AGENT'
                                            )
                                        )
);

-- ------------------------------------------------------------
-- 6. Email verification tokens
-- Store token hashes, not raw tokens.
-- ------------------------------------------------------------

CREATE TABLE email_verification_tokens (
                                           id UUID PRIMARY KEY,

                                           user_id UUID NOT NULL,
                                           token_hash VARCHAR(255) NOT NULL UNIQUE,

                                           expires_at TIMESTAMPTZ NOT NULL,
                                           used_at TIMESTAMPTZ,

                                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                           CONSTRAINT fk_email_verification_user
                                               FOREIGN KEY (user_id)
                                                   REFERENCES bank_users (id)
                                                   ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_user_id
    ON email_verification_tokens (user_id);

CREATE INDEX idx_email_verification_expires_at
    ON email_verification_tokens (expires_at);

-- ------------------------------------------------------------
-- 7. Password reset tokens
-- Store token hashes, not raw tokens.
-- ------------------------------------------------------------

CREATE TABLE password_reset_tokens (
                                       id UUID PRIMARY KEY,

                                       user_id UUID NOT NULL,
                                       token_hash VARCHAR(255) NOT NULL UNIQUE,

                                       expires_at TIMESTAMPTZ NOT NULL,
                                       used_at TIMESTAMPTZ,

                                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_password_reset_user
                                           FOREIGN KEY (user_id)
                                               REFERENCES bank_users (id)
                                               ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_user_id
    ON password_reset_tokens (user_id);

CREATE INDEX idx_password_reset_expires_at
    ON password_reset_tokens (expires_at);

-- ------------------------------------------------------------
-- 8. Refresh tokens
-- Store a hash of each refresh token.
-- ------------------------------------------------------------

CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY,

                                user_id UUID NOT NULL,
                                token_hash VARCHAR(255) NOT NULL UNIQUE,

                                expires_at TIMESTAMPTZ NOT NULL,
                                revoked_at TIMESTAMPTZ,

                                replaced_by_token_id UUID,

                                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES bank_users (id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_refresh_tokens_replacement
                                    FOREIGN KEY (replaced_by_token_id)
                                        REFERENCES refresh_tokens (id)
                                        ON DELETE SET NULL
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens (expires_at);

-- ------------------------------------------------------------
-- 9. Initial role definitions
-- ------------------------------------------------------------

INSERT INTO roles (id, name, description)
VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'CUSTOMER',
        'Bank customer'
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'STAFF',
        'General bank staff'
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'CASHIER',
        'Cashier permissions'
    ),
    (
        '10000000-0000-0000-0000-000000000004',
        'MANAGER',
        'Bank manager permissions'
    ),
    (
        '10000000-0000-0000-0000-000000000005',
        'BRANCH_MANAGER',
        'Branch manager permissions'
    ),
    (
        '10000000-0000-0000-0000-000000000006',
        'ADMIN',
        'Bank administration permissions'
    ),
    (
        '10000000-0000-0000-0000-000000000007',
        'RISK_OFFICER',
        'Risk management permissions'
    ),
    (
        '10000000-0000-0000-0000-000000000008',
        'SUPPORT_AGENT',
        'Customer support permissions'
    );
