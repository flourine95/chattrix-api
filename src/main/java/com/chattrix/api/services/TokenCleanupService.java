package com.chattrix.api.services;

import com.chattrix.api.repositories.InvalidatedTokenRepository;
import com.chattrix.api.repositories.RefreshTokenRepository;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

import java.util.logging.Logger;

@Singleton
public class TokenCleanupService {

    private static final Logger LOGGER = Logger.getLogger(TokenCleanupService.class.getName());

    @Inject
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Inject
    private RefreshTokenRepository refreshTokenRepository;

    /**
     * Tự động xóa các access token đã hết hạn khỏi blacklist mỗi giờ
     */
    @Schedule(hour = "*", persistent = false)
    public void cleanupExpiredTokens() {
        try {
            int deletedCount = invalidatedTokenRepository.deleteExpiredTokens();
            LOGGER.info("Cleaned up " + deletedCount + " expired access tokens from blacklist");
        } catch (Exception e) {
            LOGGER.severe("Error cleaning up expired access tokens: " + e.getMessage());
        }
    }

    /**
     * Tự động xóa refresh tokens đã hết hạn và đã bị revoke mỗi ngày
     */
    @Schedule(hour = "2", persistent = false)
    public void cleanupRefreshTokens() {
        try {
            int expiredCount = refreshTokenRepository.deleteExpiredTokens();
            LOGGER.info("Cleaned up " + expiredCount + " expired refresh tokens");

            int revokedCount = refreshTokenRepository.deleteRevokedTokens();
            LOGGER.info("Cleaned up " + revokedCount + " old revoked refresh tokens");
        } catch (Exception e) {
            LOGGER.severe("Error cleaning up refresh tokens: " + e.getMessage());
        }
    }
}
