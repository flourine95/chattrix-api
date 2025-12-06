package com.chattrix.api.services.auth;

import com.chattrix.api.repositories.InvalidatedTokenRepository;
import com.chattrix.api.repositories.RefreshTokenRepository;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Startup
@Slf4j
public class TokenCleanupService {

    @Inject
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Inject
    private RefreshTokenRepository refreshTokenRepository;

    @Schedule(hour = "*", persistent = false)
    public void cleanupExpiredTokens() {
        try {
            int deletedCount = invalidatedTokenRepository.deleteExpiredTokens();
            if (deletedCount > 0) {
                log.info("Cleaned up {} expired access tokens from blacklist", deletedCount);
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired access tokens: {}", e.getMessage());
        }
    }

    @Schedule(hour = "2", persistent = false)
    public void cleanupRefreshTokens() {
        try {
            int expiredCount = refreshTokenRepository.deleteExpiredTokens();
            int revokedCount = refreshTokenRepository.deleteRevokedTokens();

            if (expiredCount > 0 || revokedCount > 0) {
                log.info("Cleanup Report: {} expired tokens, {} revoked tokens cleaned", expiredCount, revokedCount);
            }
        } catch (Exception e) {
            log.error("Error cleaning up refresh tokens: {}", e.getMessage());
        }
    }
}