-- Migration for Agora Video/Audio Call API - Call History Table
-- Creates the call_history table with proper indexes for user call history

-- ============================================
-- CREATE CALL_HISTORY TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS call_history (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    call_id VARCHAR(36) NOT NULL,
    remote_user_id BIGINT NOT NULL,
    remote_user_name VARCHAR(100) NOT NULL,
    remote_user_avatar VARCHAR(500),
    call_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    duration_seconds INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_call_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_call_history_remote_user FOREIGN KEY (remote_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_call_history_call FOREIGN KEY (call_id) REFERENCES calls(id) ON DELETE CASCADE,
    CONSTRAINT uq_call_history_user_call UNIQUE (user_id, call_id),
    CONSTRAINT chk_call_history_type CHECK (call_type IN ('AUDIO', 'VIDEO')),
    CONSTRAINT chk_call_history_status CHECK (status IN ('COMPLETED', 'MISSED', 'REJECTED', 'FAILED')),
    CONSTRAINT chk_call_history_direction CHECK (direction IN ('INCOMING', 'OUTGOING'))
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_call_history_user_id ON call_history(user_id);
CREATE INDEX IF NOT EXISTS idx_call_history_timestamp ON call_history(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_call_history_user_timestamp ON call_history(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_call_history_call_id ON call_history(call_id);

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON TABLE call_history IS 'Stores call history for each user';
COMMENT ON COLUMN call_history.id IS 'Unique history entry identifier (UUID)';
COMMENT ON COLUMN call_history.user_id IS 'User who owns this history entry';
COMMENT ON COLUMN call_history.call_id IS 'Reference to the call record';
COMMENT ON COLUMN call_history.remote_user_id IS 'The other participant in the call';
COMMENT ON COLUMN call_history.remote_user_name IS 'Name of the remote user at the time of the call';
COMMENT ON COLUMN call_history.remote_user_avatar IS 'Avatar URL of the remote user at the time of the call';
COMMENT ON COLUMN call_history.call_type IS 'Type of call: AUDIO or VIDEO';
COMMENT ON COLUMN call_history.status IS 'Final status: COMPLETED, MISSED, REJECTED, or FAILED';
COMMENT ON COLUMN call_history.direction IS 'Call direction: INCOMING or OUTGOING';
COMMENT ON COLUMN call_history.timestamp IS 'When the call occurred';
COMMENT ON COLUMN call_history.duration_seconds IS 'Duration of the call in seconds (null if not completed)';
COMMENT ON COLUMN call_history.created_at IS 'When the history entry was created';
