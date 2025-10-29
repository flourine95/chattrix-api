package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "verification_tokens", indexes = {
        @Index(name = "idx_verification_token", columnList = "token"),
        @Index(name = "idx_verification_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant verifiedAt;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed = false;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isUsed && !isExpired() && verifiedAt == null;
    }

    public void markAsUsed() {
        this.isUsed = true;
        this.verifiedAt = Instant.now();
    }
}
