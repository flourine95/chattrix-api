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
    
    // Mute fields
    @Builder.Default
    @Column(name = "muted", nullable = false)
    private boolean muted = false;
    
    @Column(name = "muted_until")
    private Instant mutedUntil;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "muted_by")
    private User mutedBy;
    
    @Column(name = "muted_at")
    private Instant mutedAt;

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

    public enum Role {
        ADMIN,
        MEMBER
    }
}