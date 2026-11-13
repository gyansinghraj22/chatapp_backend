CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    full_name VARCHAR(100),
    profile_picture_url TEXT,
    bio VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_username_not_blank CHECK (length(trim(username)) > 0),
    CONSTRAINT ck_users_email_not_blank CHECK (length(trim(email)) > 0)
);

CREATE UNIQUE INDEX ux_users_username_lower ON users (lower(username));
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_expiration ON refresh_tokens (expires_at)
    WHERE revoked_at IS NULL;

CREATE TABLE chat_rooms (
    id UUID PRIMARY KEY,
    name VARCHAR(100),
    room_type VARCHAR(10) NOT NULL,
    description VARCHAR(500),
    group_picture_url TEXT,
    created_by UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    direct_chat_key VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_chat_rooms_type CHECK (room_type IN ('PRIVATE', 'GROUP')),
    CONSTRAINT ck_chat_rooms_direct_key CHECK (
        (room_type = 'PRIVATE' AND direct_chat_key IS NOT NULL)
        OR (room_type = 'GROUP' AND direct_chat_key IS NULL)
    )
);

CREATE UNIQUE INDEX ux_chat_rooms_direct_chat_key ON chat_rooms (direct_chat_key)
    WHERE direct_chat_key IS NOT NULL;
CREATE INDEX ix_chat_rooms_created_by ON chat_rooms (created_by);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    client_message_id UUID NOT NULL,
    room_id UUID NOT NULL REFERENCES chat_rooms (id) ON DELETE RESTRICT,
    sender_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    content TEXT,
    message_type VARCHAR(10) NOT NULL,
    reply_to_message_id UUID REFERENCES messages (id) ON DELETE SET NULL,
    is_edited BOOLEAN NOT NULL DEFAULT FALSE,
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_messages_type CHECK (
        message_type IN ('TEXT', 'IMAGE', 'VIDEO', 'FILE', 'AUDIO', 'SYSTEM')
    ),
    CONSTRAINT ck_messages_content_or_attachment CHECK (
        content IS NOT NULL OR message_type IN ('IMAGE', 'VIDEO', 'FILE', 'AUDIO')
    ),
    CONSTRAINT ck_messages_edit_timestamp CHECK (
        (is_edited = FALSE AND edited_at IS NULL)
        OR (is_edited = TRUE AND edited_at IS NOT NULL)
    ),
    CONSTRAINT uq_messages_sender_client_id UNIQUE (sender_id, client_message_id)
);

CREATE INDEX ix_messages_room_cursor ON messages (room_id, created_at DESC, id DESC);
CREATE INDEX ix_messages_sender ON messages (sender_id, created_at DESC);
CREATE INDEX ix_messages_reply_to ON messages (reply_to_message_id)
    WHERE reply_to_message_id IS NOT NULL;

CREATE TABLE chat_participants (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES chat_rooms (id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    role VARCHAR(10) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at TIMESTAMPTZ,
    last_read_at TIMESTAMPTZ,
    last_read_message_id UUID REFERENCES messages (id) ON DELETE SET NULL,
    is_muted BOOLEAN NOT NULL DEFAULT FALSE,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_chat_participants_role CHECK (role IN ('MEMBER', 'ADMIN', 'OWNER')),
    CONSTRAINT ck_chat_participants_read_cursor CHECK (
        (last_read_at IS NULL AND last_read_message_id IS NULL)
        OR (last_read_at IS NOT NULL AND last_read_message_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX ux_chat_participants_active_room_user
    ON chat_participants (room_id, user_id)
    WHERE left_at IS NULL;
CREATE INDEX ix_chat_participants_active_user_room
    ON chat_participants (user_id, room_id)
    WHERE left_at IS NULL;
CREATE INDEX ix_chat_participants_room_history
    ON chat_participants (room_id, joined_at DESC);

CREATE TABLE message_attachments (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    storage_key TEXT NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_message_attachments_file_size CHECK (file_size >= 0)
);

CREATE INDEX ix_message_attachments_message_id ON message_attachments (message_id);

CREATE TABLE message_reactions (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    reaction VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_message_reactions_message_user_reaction
        UNIQUE (message_id, user_id, reaction)
);

CREATE INDEX ix_message_reactions_message_id ON message_reactions (message_id);

CREATE TABLE message_receipts (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_message_receipts_message_recipient UNIQUE (message_id, recipient_id),
    CONSTRAINT ck_message_receipts_read_implies_delivery
        CHECK (read_at IS NULL OR delivered_at IS NOT NULL)
);

CREATE INDEX ix_message_receipts_recipient ON message_receipts (recipient_id, message_id);

CREATE TABLE blocked_users (
    id UUID PRIMARY KEY,
    blocker_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    blocked_user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_blocked_users_pair UNIQUE (blocker_id, blocked_user_id),
    CONSTRAINT ck_blocked_users_not_self CHECK (blocker_id <> blocked_user_id)
);

CREATE INDEX ix_blocked_users_blocked_user ON blocked_users (blocked_user_id);

CREATE TABLE message_reports (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES messages (id) ON DELETE RESTRICT,
    reporter_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT ck_message_reports_status
        CHECK (status IN ('PENDING', 'REVIEWED', 'RESOLVED', 'REJECTED')),
    CONSTRAINT uq_message_reports_message_reporter UNIQUE (message_id, reporter_id),
    CONSTRAINT ck_message_reports_review_timestamp CHECK (
        (status = 'PENDING' AND reviewed_at IS NULL)
        OR (status <> 'PENDING' AND reviewed_at IS NOT NULL)
    )
);

CREATE INDEX ix_message_reports_status_created
    ON message_reports (status, created_at DESC);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    sender_id UUID REFERENCES users (id) ON DELETE SET NULL,
    room_id UUID REFERENCES chat_rooms (id) ON DELETE SET NULL,
    message_id UUID REFERENCES messages (id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    notification_type VARCHAR(30) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_notifications_user_read_created
    ON notifications (user_id, is_read, created_at DESC);

CREATE TABLE device_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    token_ciphertext TEXT NOT NULL,
    platform VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_device_tokens_platform
        CHECK (platform IN ('ANDROID', 'IOS', 'WEB'))
);

CREATE INDEX ix_device_tokens_user_active ON device_tokens (user_id)
    WHERE is_active = TRUE;

CREATE TABLE notification_outbox (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL UNIQUE
        REFERENCES notifications (id) ON DELETE CASCADE,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_notification_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY', 'SUCCEEDED', 'DEAD')),
    CONSTRAINT ck_notification_outbox_attempts CHECK (attempts >= 0)
);

CREATE INDEX ix_notification_outbox_dispatch
    ON notification_outbox (status, next_attempt_at)
    WHERE status IN ('PENDING', 'RETRY');
