-- Notification preferences, reliable outbox claim lease, and FCM dispatch metadata.
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    message_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    group_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE notification_outbox ADD COLUMN claimed_at TIMESTAMPTZ;
ALTER TABLE notification_outbox ADD COLUMN dispatched_at TIMESTAMPTZ;
CREATE INDEX ix_notification_outbox_claim ON notification_outbox(status, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'RETRY');
