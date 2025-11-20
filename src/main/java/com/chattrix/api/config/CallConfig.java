package com.chattrix.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

import java.util.logging.Logger;

/**
 * Configuration class for call-related settings.
 * Loads call timeout and duration settings.
 */
@ApplicationScoped
@Getter
public class CallConfig {

    private static final Logger LOGGER = Logger.getLogger(CallConfig.class.getName());

    @Inject
    private AppConfig appConfig;

    private int callTimeoutSeconds;
    private int maxCallDurationSeconds;

    @PostConstruct
    public void init() {
        // Load call timeout setting (default: 60 seconds)
        callTimeoutSeconds = appConfig.getInt("call.timeout.seconds", 60);

        // Load max call duration setting (default: 4 hours = 14400 seconds)
        maxCallDurationSeconds = appConfig.getInt("call.max.duration.seconds", 14400);

        LOGGER.info("CallConfig initialized successfully");
        LOGGER.info("Call Timeout: " + callTimeoutSeconds + " seconds");
        LOGGER.info("Max Call Duration: " + maxCallDurationSeconds + " seconds");
    }
}
