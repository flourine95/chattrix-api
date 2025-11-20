-- Migration for Agora Video/Audio Call API - Call Quality Metrics Table
-- Creates the call_quality_metrics table with proper indexes and foreign key relationships

-- ============================================
-- CREATE CALL_QUALITY_METRICS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS call_quality_metrics (
    id VARCHAR(36) PRIMARY KEY,
    call_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    network_quality VARCHAR(20),
    packet_loss_rate DECIMAL(5,4),
    round_trip_time INTEGER,
    recorded_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_quality_metrics_call FOREIGN KEY (call_id) REFERENCES calls(id) ON DELETE CASCADE,
    CONSTRAINT fk_quality_metrics_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_network_quality CHECK (network_quality IN ('EXCELLENT', 'GOOD', 'POOR', 'BAD', 'VERY_BAD', 'UNKNOWN')),
    CONSTRAINT chk_packet_loss_rate CHECK (packet_loss_rate >= 0 AND packet_loss_rate <= 1),
    CONSTRAINT chk_round_trip_time CHECK (round_trip_time >= 0)
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_quality_call_id ON call_quality_metrics(call_id);
CREATE INDEX IF NOT EXISTS idx_quality_recorded_at ON call_quality_metrics(recorded_at);
CREATE INDEX IF NOT EXISTS idx_quality_user_id ON call_quality_metrics(user_id);
CREATE INDEX IF NOT EXISTS idx_quality_call_user ON call_quality_metrics(call_id, user_id);

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON TABLE call_quality_metrics IS 'Stores quality metrics reported during calls';
COMMENT ON COLUMN call_quality_metrics.id IS 'Unique metrics entry identifier (UUID)';
COMMENT ON COLUMN call_quality_metrics.call_id IS 'Reference to the call record (cascade delete)';
COMMENT ON COLUMN call_quality_metrics.user_id IS 'User who reported these metrics';
COMMENT ON COLUMN call_quality_metrics.network_quality IS 'Network quality rating: EXCELLENT, GOOD, POOR, BAD, VERY_BAD, UNKNOWN';
COMMENT ON COLUMN call_quality_metrics.packet_loss_rate IS 'Packet loss rate (0.0 to 1.0)';
COMMENT ON COLUMN call_quality_metrics.round_trip_time IS 'Round trip time in milliseconds';
COMMENT ON COLUMN call_quality_metrics.recorded_at IS 'When the metrics were recorded';
