package com.chattrix.api.services.auth;

import com.chattrix.api.config.JwtConfig;
import com.chattrix.api.entities.RefreshToken;
import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.InvalidatedTokenRepository;
import com.chattrix.api.repositories.RefreshTokenRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@ApplicationScoped
public class TokenService {

    @Inject
    private JwtConfig jwtConfig;

    @Inject
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Inject
    private RefreshTokenRepository refreshTokenRepository;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // Generate SecretKey from JWT secret string
        this.key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user, String jti) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtConfig.getAccessExpirationMillis());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .id(jti)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    public RefreshToken generateRefreshToken(User user, String accessTokenId, String accessToken) {
        // Generate unique refresh token
        String refreshTokenString = UUID.randomUUID().toString();
        
        Instant expiresAt = Instant.now().plusMillis(jwtConfig.getRefreshExpirationMillis());
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(user)
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .revoked(false)
                .build();
        refreshToken.setAccessTokenId(accessTokenId);
        refreshToken.setAccessToken(accessToken);
        refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public long getAccessTokenValidityInSeconds() {
        return jwtConfig.getAccessExpirationMillis() / 1000;
    }

    public long getRefreshTokenValidityInSeconds() {
        return jwtConfig.getRefreshExpirationMillis() / 1000;
    }

    public Long getUserIdFromToken(String token) {
        String subject = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.parseLong(subject);
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("username", String.class);
    }

    public Date getExpirationFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }

    public String getTokenId(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getId();
    }

    public boolean validateToken(String token) {
        try {
            if (invalidatedTokenRepository.isTokenInvalidated(token)) {
                return false;
            }

            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
