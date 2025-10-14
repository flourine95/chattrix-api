package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "invalidated_tokens")
public class InvalidatedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 512)
    private String token;

    @Column(name = "invalidated_at", nullable = false)
    private Instant invalidatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public InvalidatedToken() {
    }

    public InvalidatedToken(String token, Instant expiresAt) {
        this.token = token;
        this.invalidatedAt = Instant.now();
        this.expiresAt = expiresAt;
    }
}

