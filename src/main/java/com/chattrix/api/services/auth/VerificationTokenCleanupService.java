package com.chattrix.api.services.auth;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Startup
@Slf4j
public class VerificationTokenCleanupService {

    @Inject
    private VerificationService verificationService;

    @Schedule(hour = "*", persistent = false)
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired verification and password reset tokens...");
        verificationService.cleanupExpiredTokens();
        log.info("Cleanup completed.");
    }
}