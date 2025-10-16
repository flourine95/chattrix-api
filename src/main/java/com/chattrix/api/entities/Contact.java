package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "contacts",
        indexes = {
                @Index(name = "idx_contacts_user_id", columnList = "user_id"),
                @Index(name = "idx_contacts_contact_user_id", columnList = "contact_user_id")
        }
)
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_user_id", nullable = false)
    private User contactUser;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "is_favorite", nullable = false)
    private boolean isFavorite = false;

    @Column(name = "is_blocked", nullable = false)
    private boolean isBlocked = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onPrePersist() {
        this.createdAt = this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}
