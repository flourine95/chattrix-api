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

    @Builder.Default
    @Column(name = "unread_count", nullable = false)
    private int unreadCount = 0;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    // Unread marker: ID tin nhắn người dùng chọn "Đánh dấu là chưa đọc"
    @Column(name = "unread_marker_id")
    private Long unreadMarkerId;

    // Gộp từ ConversationSettings
    @Builder.Default
    @Column(name = "muted", nullable = false)
    private boolean muted = false;

    @Column(name = "muted_until")
    private Instant mutedUntil;

    @Column(name = "muted_at")
    private Instant mutedAt;

    @Builder.Default
    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Builder.Default
    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "pin_order")
    private Integer pinOrder;

    @Column(name = "pinned_at")
    private Instant pinnedAt;

    @Column(name = "theme", length = 50)
    private String theme;

    @Column(name = "custom_nickname", length = 100)
    private String customNickname;

    @Builder.Default
    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    @PrePersist
    protected void onPrePersist() {
        this.joinedAt = Instant.now();
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

    public enum Role {
        ADMIN,
        MEMBER
    }
}