package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity representing quality metrics reported during a call.
 * Multiple metrics can be recorded per call as quality changes over time.
 * Cascade delete: when a call is deleted, all associated quality metrics are deleted.
 */
@Getter
@Setter
@Entity
@Table(
        name = "call_quality_metrics",
        indexes = {
                @Index(name = "idx_quality_call_id", columnList = "call_id"),
                @Index(name = "idx_quality_recorded_at", columnList = "recorded_at"),
                @Index(name = "idx_quality_user_id", columnList = "user_id"),
                @Index(name = "idx_quality_call_user", columnList = "call_id, user_id")
        }
)
public class CallQualityMetrics {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "call_id", nullable = false, length = 36)
    private String callId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "network_quality", length = 20)
    private NetworkQuality networkQuality;

    @Column(name = "packet_loss_rate")
    private Double packetLossRate;

    @Column(name = "round_trip_time")
    private Integer roundTripTime;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
