-- Migration for Agora Video/Audio Call API - Calls Table
-- Creates the calls table with proper indexes for call management

-- ============================================
-- CREATE CALLS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS calls (
    id VARCHAR(36) PRIMARY KEY,
    channel_id VARCHAR(64) NOT NULL,
    caller_id BIGINT NOT NULL,
    callee_id BIGINT NOT NULL,
    call_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_seconds INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_calls_caller FOREIGN KEY (caller_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_calls_callee FOREIGN KEY (callee_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_call_type CHECK (call_type IN ('AUDIO', 'VIDEO')),
    CONSTRAINT chk_call_status CHECK (status IN ('INITIATING', 'RINGING', 'CONNECTING', 'CONNECTED', 'DISCONNECTING', 'ENDED', 'MISSED', 'REJECTED', 'FAILED'))
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_calls_caller_id ON calls(caller_id);
CREATE INDEX IF NOT EXISTS idx_calls_callee_id ON calls(callee_id);
CREATE INDEX IF NOT EXISTS idx_calls_status ON calls(status);
CREATE INDEX IF NOT EXISTS idx_calls_channel_id ON calls(channel_id);
CREATE INDEX IF NOT EXISTS idx_calls_start_time ON calls(start_time);

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON TABLE calls IS 'Stores active and historical call records';
COMMENT ON COLUMN calls.id IS 'Unique call identifier (UUID)';
COMMENT ON COLUMN calls.channel_id IS 'Agora channel ID for the call';
COMMENT ON COLUMN calls.caller_id IS 'User ID of the caller';
COMMENT ON COLUMN calls.callee_id IS 'User ID of the callee';
COMMENT ON COLUMN calls.call_type IS 'Type of call: AUDIO or VIDEO';
COMMENT ON COLUMN calls.status IS 'Current status of the call';
COMMENT ON COLUMN calls.start_time IS 'When the call was connected (status became CONNECTED)';
COMMENT ON COLUMN calls.end_time IS 'When the call ended';
COMMENT ON COLUMN calls.duration_seconds IS 'Duration of the call in seconds';
COMMENT ON COLUMN calls.created_at IS 'When the call record was created';
COMMENT ON COLUMN calls.updated_at IS 'When the call record was last updated';
