-- Password reset secrets are single-use hashes; OAuth identities retain verified provider subjects.
CREATE TABLE oauth_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('GOOGLE')),
    subject VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_oauth_provider_subject UNIQUE (provider, subject),
    CONSTRAINT uk_oauth_user_provider UNIQUE (user_id, provider)
);
CREATE INDEX ix_oauth_identities_user ON oauth_identities(user_id);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_password_reset_user_created ON password_reset_tokens(user_id, created_at DESC);
CREATE INDEX ix_password_reset_expiry ON password_reset_tokens(expires_at) WHERE used_at IS NULL;
