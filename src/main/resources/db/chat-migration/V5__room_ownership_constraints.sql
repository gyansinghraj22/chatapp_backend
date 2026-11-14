-- Enforce no more than one active owner in a group. Ownership transfer demotes then promotes in one transaction.
CREATE UNIQUE INDEX ux_chat_participants_one_active_owner
    ON chat_participants(room_id)
    WHERE left_at IS NULL AND role = 'OWNER';
CREATE INDEX ix_chat_participants_room_active_joined
    ON chat_participants(room_id, joined_at ASC)
    WHERE left_at IS NULL;
