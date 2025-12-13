package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "calls", indexes = {
        @Index(name = "idx_calls_caller_id", columnList = "caller_id"),
        @Index(name = "idx_calls_callee_id", columnList = "callee_id"),
        @Index(name = "idx_calls_status", columnList = "status"),
        @Index(name = "idx_calls_channel_id", columnList = "channel_id")
})
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

    @PrePersist
    protected void onPrePersist() {
        this.createdAt = this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    public void accept() {
        if (this.status != CallStatus.RINGING) {
            throw new IllegalStateException("Cannot accept call in status: " + this.status);
        }
        this.status = CallStatus.CONNECTING;
        this.startTime = Instant.now();
    }

    public void end(CallStatus endStatus) {
        if (isFinished()) return;

        this.status = endStatus;
        this.endTime = Instant.now();

        if (this.startTime != null && this.endTime != null) {
            this.durationSeconds = (int) Duration.between(this.startTime, this.endTime).getSeconds();
        } else {
            this.durationSeconds = 0;
        }
    }

    public boolean isCaller(Long userId) {
        return this.callerId != null && this.callerId.equals(userId);
    }

    public boolean isCallee(Long userId) {
        return this.calleeId != null && this.calleeId.equals(userId);
    }

    public boolean isParticipant(Long userId) {
        return isCaller(userId) || isCallee(userId);
    }

    public Long getOtherUserId(Long currentUserId) {
        if (isCaller(currentUserId)) return this.calleeId;
        if (isCallee(currentUserId)) return this.callerId;
        throw new IllegalArgumentException("User " + currentUserId + " is not a participant");
    }

    public boolean isFinished() {
        return this.status == CallStatus.ENDED
                || this.status == CallStatus.REJECTED
                || this.status == CallStatus.MISSED;
    }
}