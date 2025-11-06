package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "pinned_messages",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"conversation_id", "message_id"})
        },
        indexes = {
                @Index(name = "idx_pinned_messages_conversation", columnList = "conversation_id"),
                @Index(name = "idx_pinned_messages_message", columnList = "message_id"),
                @Index(name = "idx_pinned_messages_order", columnList = "conversation_id, pin_order")
        }
)
public class PinnedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_by", nullable = false)
    private User pinnedBy;

    @Column(name = "pinned_at", nullable = false)
    private Instant pinnedAt;

    @Column(name = "pin_order")
    private Integer pinOrder;

    @PrePersist
    protected void onPrePersist() {
        if (this.pinnedAt == null) {
            this.pinnedAt = Instant.now();
        }
    }
}

