-- Uploaded media is staged until explicitly attached to a message. The object bytes live
-- in private local/S3-compatible storage; only metadata and opaque storage keys are stored here.
CREATE TABLE media_uploads (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    storage_key VARCHAR(512) NOT NULL UNIQUE,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    sha256 CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED'
        CHECK (status IN ('UPLOADED', 'ATTACHED', 'DELETED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_media_uploads_owner_created ON media_uploads(owner_id, created_at DESC);
CREATE INDEX ix_media_uploads_expiration ON media_uploads(expires_at) WHERE status = 'UPLOADED';
