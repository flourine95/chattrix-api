package com.chattrix.api.services;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

@Singleton
public class UserStatusCleanupService {

    @Inject
    private UserStatusService userStatusService;

    /**
     * Cleanup stale online statuses every 5 minutes
     * This will mark users as offline if they haven't been seen in the last 5 minutes
     */
    @Schedule(minute = "*/5", hour = "*", persistent = false)
    public void cleanupStaleOnlineStatuses() {
        System.out.println("Running cleanup of stale online statuses...");
        try {
            userStatusService.cleanupStaleOnlineStatuses();
            System.out.println("Cleanup of stale online statuses completed successfully");
        } catch (Exception e) {
            System.err.println("Error during cleanup of stale online statuses: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
