package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "message_edit_history", indexes = {
        @Index(name = "idx_edit_history_message", columnList = "message_id"),
        @Index(name = "idx_edit_history_edited_at", columnList = "edited_at")
})
public class MessageEditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "previous_content", columnDefinition = "TEXT", nullable = false)
    private String previousContent;

    @Column(name = "edited_at", nullable = false)
    private Instant editedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by", nullable = false)
    private User editedBy;

    @PrePersist
    protected void onPrePersist() {
        this.editedAt = Instant.now();
    }
}

