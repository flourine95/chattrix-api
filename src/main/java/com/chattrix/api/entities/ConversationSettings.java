package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "conversation_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "conversation_id"})
})
public class ConversationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Builder.Default
    @Column(name = "is_muted", nullable = false)
    private boolean isMuted = false;

    @Column(name = "muted_at")
    private Instant mutedAt;

    @Column(name = "muted_until")
    private Instant mutedUntil;

    @Builder.Default
    @Column(name = "is_blocked", nullable = false)
    private boolean isBlocked = false;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Builder.Default
    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    @Column(name = "custom_nickname", length = 100)
    private String customNickname;

    @Column(name = "theme", length = 50)
    private String theme;

    @Builder.Default
    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden = false;

    @Column(name = "hidden_at")
    private Instant hiddenAt;

    @Builder.Default
    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Builder.Default
    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned = false;

    @Column(name = "pin_order")
    private Integer pinOrder;

    @Column(name = "pinned_at")
    private Instant pinnedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onPrePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}