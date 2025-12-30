package com.chattrix.api.entities;

import com.chattrix.api.enums.Gender;
import com.chattrix.api.enums.ProfileVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_email", columnList = "email")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String bio;

    @Column(name = "date_of_birth")
    private Instant dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", length = 20)
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    /**
     * TRUNG TÂM NOTE: Lưu trữ ghi chú trạng thái và nhạc.
     * Cấu trúc: { "content": "...", "emoji": "...", "expires_at": "...",
     * "music": { "track_id": "...", "start_sec": 10, "end_sec": 40 } }
     */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "note_metadata", columnDefinition = "jsonb")
    private Map<String, Object> noteMetadata = new HashMap<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private Set<ConversationParticipant> conversationParticipants = new HashSet<>();

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onPrePersist() {
        this.createdAt = this.updatedAt = Instant.now();
        this.lastSeen = Instant.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}