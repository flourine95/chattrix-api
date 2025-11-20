package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity representing a call history entry for a user.
 * Each call generates two history entries - one for the caller and one for the callee.
 */
@Getter
@Setter
@Entity
@Table(
        name = "call_history",
        indexes = {
                @Index(name = "idx_call_history_user_id", columnList = "user_id"),
                @Index(name = "idx_call_history_timestamp", columnList = "timestamp DESC"),
                @Index(name = "idx_call_history_user_timestamp", columnList = "user_id, timestamp DESC"),
                @Index(name = "idx_call_history_call_id", columnList = "call_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_call_history_user_call", columnNames = {"user_id", "call_id"})
        }
)
public class CallHistory {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "call_id", nullable = false, length = 36)
    private String callId;

    @Column(name = "remote_user_id", nullable = false)
    private Long remoteUserId;

    @Column(name = "remote_user_name", nullable = false, length = 100)
    private String remoteUserName;

    @Column(name = "remote_user_avatar", length = 500)
    private String remoteUserAvatar;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false, length = 10)
    private CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallHistoryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CallDirection direction;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Lifecycle callback executed before persisting a new entity.
     * Sets createdAt to current UTC time.
     */
    @PrePersist
    protected void onPrePersist() {
        this.createdAt = Instant.now();
    }
}
