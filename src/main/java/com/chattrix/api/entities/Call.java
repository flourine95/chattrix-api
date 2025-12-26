package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "calls", indexes = {
        @Index(name = "idx_calls_caller_id", columnList = "caller_id"),
        @Index(name = "idx_calls_conversation_id", columnList = "conversation_id"),
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

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false, length = 10)
    private CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallStatus status;

    @OneToMany(mappedBy = "call", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CallParticipant> participants = new ArrayList<>();

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

    public void accept(Long userId) {
        participants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .ifPresent(p -> {
                    p.setStatus(ParticipantStatus.JOINED);
                    p.setJoinedAt(Instant.now());
                });

        if (this.status == CallStatus.RINGING) {
            this.status = CallStatus.CONNECTED;
            this.startTime = Instant.now();
        }
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

    public boolean isParticipant(Long userId) {
        return isCaller(userId) || participants.stream().anyMatch(p -> p.getUserId().equals(userId));
    }

    public boolean isFinished() {
        return this.status == CallStatus.ENDED
                || this.status == CallStatus.REJECTED
                || this.status == CallStatus.MISSED;
    }
}
