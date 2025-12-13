package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Messenger Notes - Status ngắn (tối đa 60 ký tự) hiển thị trên avatar
 * Tự động biến mất sau 24 giờ
 */
@Getter
@Setter
@Entity
@Table(name = "user_notes",
        indexes = {
                @Index(name = "idx_user_notes_user", columnList = "user_id"),
                @Index(name = "idx_user_notes_expires_at", columnList = "expires_at")
        }
)
public class UserNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "note_text", nullable = false, length = 60)
    private String noteText;

    // Music/emoji that can be attached to the note
    @Column(name = "music_url")
    private String musicUrl;

    @Column(name = "music_title")
    private String musicTitle;

    @Column(name = "emoji")
    private String emoji;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @PrePersist
    protected void onPrePersist() {
        this.createdAt = Instant.now();
        // Auto-expire after 24 hours
        this.expiresAt = this.createdAt.plusSeconds(24 * 60 * 60);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
}

