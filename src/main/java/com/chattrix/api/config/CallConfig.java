package com.chattrix.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;

import java.util.logging.Logger;

/**
 * Configuration class for call-related settings.
 * Loads call timeout and duration settings from environment variables.
 */
@ApplicationScoped
@Getter
public class CallConfig {

    private static final Logger LOGGER = Logger.getLogger(CallConfig.class.getName());

    private int callTimeoutSeconds;
    private int maxCallDurationSeconds;

    @PostConstruct
    public void init() {
        // Load call timeout setting (default: 60 seconds)
        callTimeoutSeconds = getEnvAsInt("CALL_TIMEOUT_SECONDS", 60);

        // Load max call duration setting (default: 4 hours = 14400 seconds)
        maxCallDurationSeconds = getEnvAsInt("CALL_MAX_DURATION_SECONDS", 14400);

        LOGGER.info("CallConfig initialized successfully");
        LOGGER.info("Call Timeout: " + callTimeoutSeconds + " seconds");
        LOGGER.info("Max Call Duration: " + maxCallDurationSeconds + " seconds");
    }

    /**
     * Helper method to get environment variable as integer with default value
     */
    private int getEnvAsInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid integer value for " + key + ": " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }
}
