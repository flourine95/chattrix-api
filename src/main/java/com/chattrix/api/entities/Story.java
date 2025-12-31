package com.chattrix.api.entities;

import com.chattrix.api.enums.StoryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stories", indexes = {
        @Index(name = "idx_stories_user", columnList = "user_id"),
        @Index(name = "idx_stories_expires", columnList = "expires_at")
})
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "media_url", nullable = false, columnDefinition = "TEXT")
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private StoryType type; // IMAGE, VIDEO

    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;

    /**
     * TRUNG TÂM TƯƠNG TÁC:
     * 1. Music: Lưu track_id, start_at, end_at (nếu có nhạc kèm story).
     * 2. Viewers & Reactions: Lưu danh sách người xem và các emoji họ đã thả.
     */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private List<Map<String, Object>> metadata = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @PrePersist
    protected void onPrePersist() {
        this.createdAt = Instant.now();
        // Mặc định hết hạn sau 24 giờ
        if (this.expiresAt == null) {
            this.expiresAt = Instant.now().plus(Duration.ofHours(24));
        }
    }
}