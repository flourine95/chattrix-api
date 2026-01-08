package com.chattrix.api.services.message;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Startup
@Slf4j
public class ScheduledMessageProcessorService {

    @Inject
    private ScheduledMessageService scheduledMessageService;

    @Schedule(second = "*/10", minute = "*", hour = "*", persistent = false)
    public void processScheduledMessages() {
        try {
            scheduledMessageService.processScheduledMessages();
        } catch (Exception e) {
            log.error("Error processing scheduled messages", e);
        }
    }
}