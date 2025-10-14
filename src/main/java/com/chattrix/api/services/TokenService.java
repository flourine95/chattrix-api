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

    // Generate a secure key for HS256
    private final SecretKey key = Jwts.SIG.HS256.key().build();

    // Access token: 15 phút
    private final static long ACCESS_TOKEN_VALIDITY = 900000; // 15 minutes

    // Refresh token: 7 ngày
    private final static long REFRESH_TOKEN_VALIDITY = 604800000; // 7 days

    @Inject
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Inject
    private RefreshTokenRepository refreshTokenRepository;

    public String generateAccessToken(User user, String jti) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .id(jti)  // JWT ID để link với refresh token
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    public RefreshToken generateRefreshToken(User user, String accessTokenId, String accessToken) {
        Instant expiresAt = Instant.now().plusMillis(REFRESH_TOKEN_VALIDITY);
        RefreshToken refreshToken = new RefreshToken(user, expiresAt);
        refreshToken.setAccessTokenId(accessTokenId);
        refreshToken.setAccessToken(accessToken);  // Lưu access token string
        refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public long getAccessTokenValidityInSeconds() {
        return ACCESS_TOKEN_VALIDITY / 1000;
    }

    public long getRefreshTokenValidityInSeconds() {
        return REFRESH_TOKEN_VALIDITY / 1000;
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
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
            // Kiểm tra token có trong blacklist không
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
