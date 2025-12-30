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
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 512)
    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    // Thêm field để track access token tương ứng
    @Column(name = "access_token_id", length = 100)
    private String accessTokenId;

    // Lưu access token string để có thể blacklist khi refresh
    @Column(name = "access_token", length = 1024)
    private String accessToken;

    // Metadata cho device tracking (optional)
    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }
}
