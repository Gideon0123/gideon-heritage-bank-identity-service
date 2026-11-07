CREATE TABLE staff_onboarding_tokens
(
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    token_hash VARCHAR(255) NOT NULL,

    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

    used_at TIMESTAMP WITH TIME ZONE,

    revoked_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_staff_onboarding_user
        FOREIGN KEY (user_id)
            REFERENCES bank_users(id),

    CONSTRAINT uk_staff_onboarding_token_hash
        UNIQUE (token_hash)
);

CREATE INDEX idx_staff_onboarding_user_id
    ON staff_onboarding_tokens(user_id);

CREATE INDEX idx_staff_onboarding_expires_at
    ON staff_onboarding_tokens(expires_at);