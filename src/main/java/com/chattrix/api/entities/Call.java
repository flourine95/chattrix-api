package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity representing a call session between two users.
 * Tracks the complete lifecycle of audio/video calls using Agora RTC.
 */
@Getter
@Setter
@Entity
@Table(
        name = "calls",
        indexes = {
                @Index(name = "idx_calls_caller_id", columnList = "caller_id"),
                @Index(name = "idx_calls_callee_id", columnList = "callee_id"),
                @Index(name = "idx_calls_status", columnList = "status"),
                @Index(name = "idx_calls_channel_id", columnList = "channel_id"),
                @Index(name = "idx_calls_start_time", columnList = "start_time")
        }
)
public class Call {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "channel_id", nullable = false, length = 64)
    private String channelId;

    @Column(name = "caller_id", nullable = false)
    private Long callerId;

    @Column(name = "callee_id", nullable = false)
    private Long calleeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false, length = 10)
    private CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallStatus status;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Lifecycle callback executed before persisting a new entity.
     * Sets createdAt and updatedAt to current UTC time.
     */
    @PrePersist
    protected void onPrePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Lifecycle callback executed before updating an existing entity.
     * Updates the updatedAt timestamp to current UTC time.
     */
    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}
