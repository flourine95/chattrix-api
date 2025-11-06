-- Migration for Chat Info Feature
-- This script adds the necessary database changes for the Chat Info feature

-- 1. Add avatar_url column to conversations table
ALTER TABLE conversations 
ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

-- 2. Create conversation_settings table
CREATE TABLE IF NOT EXISTS conversation_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    is_muted BOOLEAN NOT NULL DEFAULT FALSE,
    muted_until TIMESTAMP,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    blocked_at TIMESTAMP,
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    custom_nickname VARCHAR(150),
    theme VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversation_settings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_settings_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_conversation UNIQUE (user_id, conversation_id)
);

-- 3. Create indexes for conversation_settings
CREATE INDEX IF NOT EXISTS idx_conversation_settings_user ON conversation_settings(user_id);
CREATE INDEX IF NOT EXISTS idx_conversation_settings_conversation ON conversation_settings(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conversation_settings_muted ON conversation_settings(is_muted);
CREATE INDEX IF NOT EXISTS idx_conversation_settings_blocked ON conversation_settings(is_blocked);

-- 4. Add indexes for message search optimization
CREATE INDEX IF NOT EXISTS idx_messages_content ON messages(content);
CREATE INDEX IF NOT EXISTS idx_messages_type ON messages(type);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_type ON messages(conversation_id, type);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_sender ON messages(conversation_id, sender_id);

-- 5. Comments for documentation
COMMENT ON TABLE conversation_settings IS 'Stores per-user settings for conversations including mute, block, notifications, custom nicknames, and themes';
COMMENT ON COLUMN conversation_settings.is_muted IS 'Whether the conversation is muted for this user';
COMMENT ON COLUMN conversation_settings.muted_until IS 'Timestamp until which the conversation is muted (NULL for indefinite mute)';
COMMENT ON COLUMN conversation_settings.is_blocked IS 'Whether the user is blocked in this conversation (DIRECT conversations only)';
COMMENT ON COLUMN conversation_settings.blocked_at IS 'Timestamp when the user was blocked';
COMMENT ON COLUMN conversation_settings.notifications_enabled IS 'Whether notifications are enabled for this conversation';
COMMENT ON COLUMN conversation_settings.custom_nickname IS 'Custom nickname for the conversation';
COMMENT ON COLUMN conversation_settings.theme IS 'Custom theme for the conversation';
COMMENT ON COLUMN conversations.avatar_url IS 'Avatar URL for group conversations';

