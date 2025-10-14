package com.chattrix.api.services;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

import java.util.logging.Logger;

@Singleton
public class VerificationTokenCleanupService {

    private static final Logger LOGGER = Logger.getLogger(VerificationTokenCleanupService.class.getName());

    @Inject
    private VerificationService verificationService;

    /**
     * Run every hour to cleanup expired verification and password reset tokens
     */
    @Schedule(hour = "*", minute = "0", persistent = false)
    public void cleanupExpiredTokens() {
        LOGGER.info("Starting cleanup of expired verification and password reset tokens...");
        verificationService.cleanupExpiredTokens();
        LOGGER.info("Cleanup completed.");
    }
}
