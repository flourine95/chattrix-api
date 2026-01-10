package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "conversation_participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "conversation_id"})
})
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "unread_count", nullable = false)
    private int unreadCount;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    // Unread marker: ID tin nhắn người dùng chọn "Đánh dấu là chưa đọc"
    @Column(name = "unread_marker_id")
    private Long unreadMarkerId;

    // Gộp từ ConversationSettings
    @Column(name = "muted", nullable = false)
    private boolean muted;

    @Column(name = "muted_until")
    private Instant mutedUntil;

    @Column(name = "muted_at")
    private Instant mutedAt;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    @Column(name = "pin_order")
    private Integer pinOrder;

    @Column(name = "pinned_at")
    private Instant pinnedAt;

    @Column(name = "theme", length = 50)
    private String theme;

    @Column(name = "custom_nickname", length = 100)
    private String customNickname;

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled;

    @PrePersist
    protected void onPrePersist() {
        if (this.joinedAt == null) {
            this.joinedAt = Instant.now();
        }
        // Set default values for primitive boolean fields
        // These will only be set if the field wasn't explicitly initialized
        if (!muted && mutedAt == null && mutedUntil == null) {
            this.muted = false;
        }
        if (!archived && archivedAt == null) {
            this.archived = false;
        }
        if (!pinned && pinnedAt == null && pinOrder == null) {
            this.pinned = false;
        }
        if (!notificationsEnabled && this.id == null) {
            this.notificationsEnabled = true;
        }
    }

    /**
     * Check if member is currently muted
     */
    public boolean isCurrentlyMuted() {
        if (!muted) {
            return false;
        }

        // If mutedUntil is null, it's permanent mute
        if (mutedUntil == null) {
            return true;
        }

        // Check if mute has expired
        return Instant.now().isBefore(mutedUntil);
    }

    /**
     * Calculate unread count based on lastReadMessageId and unreadMarkerId
     * Logic: Nếu có unreadMarkerId, đếm từ đó. Nếu không, đếm từ lastReadMessageId
     */
    public Long getEffectiveLastReadMessageId() {
        return unreadMarkerId != null ? unreadMarkerId : lastReadMessageId;
    }

    public boolean unarchive() {
        if (this.archived) {
            this.archived = false;
            this.archivedAt = null;
            return true;
        }
        return false;
    }

    public enum Role {
        ADMIN,
        MEMBER
    }

    public boolean hasUserId(Long userId) {
        return this.user != null && this.user.hasId(userId);
    }
}