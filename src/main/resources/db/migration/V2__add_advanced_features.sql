-- Migration for Advanced Chat Features
-- Friend Request, Message Edit/Delete/Forward, Read Receipts, Unread Count, Pinned Messages, Conversation Hide/Archive/Pin

-- ============================================
-- 1. UPDATE CONTACTS TABLE - Add Friend Request Status
-- ============================================

ALTER TABLE contacts 
ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
ADD COLUMN IF NOT EXISTS requested_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP;

-- Migrate existing data: set accepted_at for existing contacts
UPDATE contacts 
SET accepted_at = created_at 
WHERE accepted_at IS NULL AND status = 'ACCEPTED';

-- Create index for status queries
CREATE INDEX IF NOT EXISTS idx_contacts_status ON contacts(status);
CREATE INDEX IF NOT EXISTS idx_contacts_user_status ON contacts(user_id, status);

-- Add check constraint for status enum
ALTER TABLE contacts 
ADD CONSTRAINT chk_contact_status 
CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'BLOCKED'));

-- ============================================
-- 2. UPDATE MESSAGES TABLE - Add Edit/Delete/Forward
-- ============================================

ALTER TABLE messages
ADD COLUMN IF NOT EXISTS is_edited BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS edited_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS deleted_by BIGINT,
ADD COLUMN IF NOT EXISTS is_forwarded BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS original_message_id BIGINT,
ADD COLUMN IF NOT EXISTS forward_count INTEGER DEFAULT 0;

-- Add foreign keys
ALTER TABLE messages
ADD CONSTRAINT fk_messages_deleted_by 
FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE messages
ADD CONSTRAINT fk_messages_original_message 
FOREIGN KEY (original_message_id) REFERENCES messages(id) ON DELETE SET NULL;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_messages_is_deleted ON messages(is_deleted);
CREATE INDEX IF NOT EXISTS idx_messages_is_edited ON messages(is_edited);
CREATE INDEX IF NOT EXISTS idx_messages_original ON messages(original_message_id);

-- ============================================
-- 3. CREATE MESSAGE_EDIT_HISTORY TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS message_edit_history (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    previous_content TEXT NOT NULL,
    edited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edited_by BIGINT NOT NULL,
    CONSTRAINT fk_edit_history_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_edit_history_user FOREIGN KEY (edited_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_edit_history_message ON message_edit_history(message_id);
CREATE INDEX IF NOT EXISTS idx_edit_history_edited_at ON message_edit_history(edited_at);

-- ============================================
-- 4. CREATE MESSAGE_READ_RECEIPTS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS message_read_receipts (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_read_receipts_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_read_receipts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_read_receipt_message_user UNIQUE (message_id, user_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_read_receipts_message ON message_read_receipts(message_id);
CREATE INDEX IF NOT EXISTS idx_read_receipts_user ON message_read_receipts(user_id);
CREATE INDEX IF NOT EXISTS idx_read_receipts_read_at ON message_read_receipts(read_at);

-- ============================================
-- 5. UPDATE CONVERSATION_PARTICIPANTS TABLE - Add Unread Count
-- ============================================

ALTER TABLE conversation_participants
ADD COLUMN IF NOT EXISTS unread_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS last_read_message_id BIGINT,
ADD COLUMN IF NOT EXISTS last_read_at TIMESTAMP;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_participants_unread ON conversation_participants(user_id, unread_count);
CREATE INDEX IF NOT EXISTS idx_participants_last_read ON conversation_participants(last_read_message_id);

-- ============================================
-- 6. CREATE PINNED_MESSAGES TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS pinned_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    pinned_by BIGINT NOT NULL,
    pinned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pin_order INTEGER,
    CONSTRAINT fk_pinned_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_pinned_messages_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_pinned_messages_user FOREIGN KEY (pinned_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_pinned_message_conversation UNIQUE (conversation_id, message_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_pinned_messages_conversation ON pinned_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_pinned_messages_message ON pinned_messages(message_id);
CREATE INDEX IF NOT EXISTS idx_pinned_messages_order ON pinned_messages(conversation_id, pin_order);

-- ============================================
-- 7. UPDATE CONVERSATION_SETTINGS TABLE - Add Hide/Archive/Pin
-- ============================================

ALTER TABLE conversation_settings
ADD COLUMN IF NOT EXISTS is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS is_archived BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS pin_order INTEGER,
ADD COLUMN IF NOT EXISTS pinned_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS muted_at TIMESTAMP;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_conversation_settings_hidden ON conversation_settings(user_id, is_hidden);
CREATE INDEX IF NOT EXISTS idx_conversation_settings_archived ON conversation_settings(user_id, is_archived);
CREATE INDEX IF NOT EXISTS idx_conversation_settings_pinned ON conversation_settings(user_id, is_pinned, pin_order);

-- ============================================
-- 8. COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON COLUMN contacts.status IS 'Friend request status: PENDING, ACCEPTED, REJECTED, BLOCKED';
COMMENT ON COLUMN contacts.requested_at IS 'When the friend request was sent';
COMMENT ON COLUMN contacts.accepted_at IS 'When the friend request was accepted';
COMMENT ON COLUMN contacts.rejected_at IS 'When the friend request was rejected';

COMMENT ON COLUMN messages.is_edited IS 'Whether the message has been edited';
COMMENT ON COLUMN messages.edited_at IS 'When the message was last edited';
COMMENT ON COLUMN messages.is_deleted IS 'Soft delete flag';
COMMENT ON COLUMN messages.deleted_at IS 'When the message was deleted';
COMMENT ON COLUMN messages.deleted_by IS 'User who deleted the message';
COMMENT ON COLUMN messages.is_forwarded IS 'Whether this message is forwarded from another';
COMMENT ON COLUMN messages.original_message_id IS 'Original message if this is a forward';
COMMENT ON COLUMN messages.forward_count IS 'Number of times this message has been forwarded';

COMMENT ON TABLE message_edit_history IS 'Stores edit history for messages';
COMMENT ON TABLE message_read_receipts IS 'Tracks which users have read which messages';
COMMENT ON TABLE pinned_messages IS 'Messages pinned in conversations';

COMMENT ON COLUMN conversation_participants.unread_count IS 'Number of unread messages for this user in this conversation';
COMMENT ON COLUMN conversation_participants.last_read_message_id IS 'Last message read by this user';
COMMENT ON COLUMN conversation_participants.last_read_at IS 'When the user last read messages';

COMMENT ON COLUMN conversation_settings.is_hidden IS 'Whether the conversation is hidden from the list';
COMMENT ON COLUMN conversation_settings.is_archived IS 'Whether the conversation is archived';
COMMENT ON COLUMN conversation_settings.is_pinned IS 'Whether the conversation is pinned to the top';
COMMENT ON COLUMN conversation_settings.pin_order IS 'Order of pinned conversations (lower = higher priority)';

