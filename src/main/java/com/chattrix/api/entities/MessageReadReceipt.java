package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "message_read_receipts",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"message_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_read_receipts_message", columnList = "message_id"),
                @Index(name = "idx_read_receipts_user", columnList = "user_id"),
                @Index(name = "idx_read_receipts_read_at", columnList = "read_at")
        }
)
public class MessageReadReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.readAt == null) {
            this.readAt = Instant.now();
        }
    }
}

