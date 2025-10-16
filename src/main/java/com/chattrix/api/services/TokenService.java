package com.chattrix.api.services;

import com.chattrix.api.entities.RefreshToken;
import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.InvalidatedTokenRepository;
import com.chattrix.api.repositories.RefreshTokenRepository;
import io.jsonwebtoken.Jwts;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@ApplicationScoped
public class TokenService {

    private static final int MINUTE = 60 * 1000;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;
    private static final long REFRESH_TOKEN_VALIDITY = 7 * DAY;
    private static final long ACCESS_TOKEN_VALIDITY = 15 * MINUTE;
    private final SecretKey key = Jwts.SIG.HS256.key().build();
    @Inject
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Inject
    private RefreshTokenRepository refreshTokenRepository;

    public String generateAccessToken(User user, String jti) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY);

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
        Instant expiresAt = Instant.now().plusMillis(REFRESH_TOKEN_VALIDITY);
        RefreshToken refreshToken = new RefreshToken(user, expiresAt);
        refreshToken.setAccessTokenId(accessTokenId);
        refreshToken.setAccessToken(accessToken);
        refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public long getAccessTokenValidityInSeconds() {
        return ACCESS_TOKEN_VALIDITY / 1000;
    }

    public long getRefreshTokenValidityInSeconds() {
        return REFRESH_TOKEN_VALIDITY / 1000;
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
