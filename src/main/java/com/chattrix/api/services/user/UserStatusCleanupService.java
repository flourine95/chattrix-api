package com.chattrix.api.services.user;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

@Singleton
public class UserStatusCleanupService {

    @Inject
    private UserStatusService userStatusService;

    /**
     * Cleanup stale online statuses every 1 minute
     * This will mark users as offline if they haven't been seen in the last 2 minutes
     */
    @Schedule(minute = "*/1", hour = "*", persistent = false)
    public void cleanupStaleOnlineStatuses() {
        try {
            userStatusService.cleanupStaleOnlineStatuses();
            System.out.println("Cleanup of stale online statuses completed successfully");
        } catch (Exception e) {
            System.err.println("Error during cleanup of stale online statuses: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
