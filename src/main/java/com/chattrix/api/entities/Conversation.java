package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String name; // Tên nhóm, null nếu 1-1

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl; // Avatar URL cho nhóm

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type; // DIRECT hoặc GROUP

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messages;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ConversationParticipant> participants;

    @OneToOne
    @JoinColumn(name = "last_message_id")
    private Message lastMessage;

    @Column(name = "created_at", updatable = false)
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

    public enum ConversationType {
        DIRECT, // 1-1
        GROUP   // Nhiều người
    }
}
