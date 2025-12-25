package com.chattrix.api.services.message;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.util.logging.Logger;

@Singleton
@Startup
public class ScheduledMessageProcessorService {

    private static final Logger LOGGER = Logger.getLogger(ScheduledMessageProcessorService.class.getName());

    @Inject
    private ScheduledMessageService scheduledMessageService;

    /**
     * Process scheduled messages every 30 seconds
     */
    @Schedule(second = "*/30", minute = "*", hour = "*", persistent = false)
    public void processScheduledMessages() {
        try {
            scheduledMessageService.processScheduledMessages();
            LOGGER.info("Scheduled messages processed successfully");
        } catch (Exception e) {
            LOGGER.severe("Error processing scheduled messages: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
