-- M1: user accounts (R1) and refresh tokens for JWT sessions.

CREATE TABLE users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(254) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    display_name    VARCHAR(50)  NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    daily_new_limit INTEGER      NOT NULL DEFAULT 20,
    timezone        VARCHAR(64)  NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    onboarded_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    -- R1: email is unique; it is always stored lower-cased so uniqueness is case-insensitive
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email)),
    CONSTRAINT ck_users_role CHECK (role IN ('LEARNER', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED')),
    CONSTRAINT ck_users_daily_new_limit CHECK (daily_new_limit BETWEEN 0 AND 200)
);

CREATE TABLE refresh_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- only the SHA-256 hash (hex) is stored, never the raw token
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
