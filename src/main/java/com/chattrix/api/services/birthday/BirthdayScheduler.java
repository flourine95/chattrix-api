package com.chattrix.api.services.birthday;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Scheduled job to automatically check and send birthday wishes
 * Runs every day at midnight (00:00)
 */
@Singleton
@Startup
public class BirthdayScheduler {

    @Inject
    BirthdayService birthdayService;

    /**
     * Check for birthdays and send wishes automatically
     * Runs at 00:00 every day
     * 
     * Cron format: second minute hour dayOfMonth month dayOfWeek year
     * "0 0 0 * * *" = Every day at midnight
     */
    @Schedule(hour = "0", minute = "0", second = "0", persistent = false)
    public void checkBirthdaysDaily() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.out.println("[Birthday Scheduler] Starting birthday check at: " + timestamp);
        
        try {
            birthdayService.checkAndSendBirthdayWishes();
            System.out.println("[Birthday Scheduler] Birthday check completed successfully");
        } catch (Exception e) {
            System.err.println("[Birthday Scheduler] Error during birthday check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Optional: Run every hour for testing purposes
     * Uncomment this method to test the scheduler more frequently
     */
    @Schedule(hour = "*", minute = "0", second = "0", persistent = false)
    public void checkBirthdaysHourly() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.out.println("[Birthday Scheduler - Hourly] Starting birthday check at: " + timestamp);
        
        try {
            birthdayService.checkAndSendBirthdayWishes();
            System.out.println("[Birthday Scheduler - Hourly] Birthday check completed successfully");
        } catch (Exception e) {
            System.err.println("[Birthday Scheduler - Hourly] Error during birthday check: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
