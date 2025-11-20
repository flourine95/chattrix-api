package com.chattrix.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;

import java.util.logging.Logger;

/**
 * Configuration class for Agora RTC SDK settings.
 * Loads Agora App ID, App Certificate, and token expiration settings from environment variables.
 */
@ApplicationScoped
@Getter
public class AgoraConfig {

    private static final Logger LOGGER = Logger.getLogger(AgoraConfig.class.getName());

    private String appId;
    private String appCertificate;
    private int defaultTokenExpiration;
    private int maxTokenExpiration;
    private int minTokenExpiration;

    @PostConstruct
    public void init() {
        // Load Agora App ID from environment (Required)
        appId = System.getenv("AGORA_APP_ID");
        if (appId == null || appId.isBlank()) {
            throw new IllegalStateException("AGORA_APP_ID environment variable is required but not set");
        }

        // Load Agora App Certificate from environment (Required)
        appCertificate = System.getenv("AGORA_APP_CERTIFICATE");
        if (appCertificate == null || appCertificate.isBlank()) {
            throw new IllegalStateException("AGORA_APP_CERTIFICATE environment variable is required but not set");
        }

        // Load token expiration settings with defaults
        defaultTokenExpiration = getEnvAsInt("AGORA_DEFAULT_TOKEN_EXPIRATION", 3600); // 1 hour default
        maxTokenExpiration = getEnvAsInt("AGORA_MAX_TOKEN_EXPIRATION", 86400); // 24 hours max
        minTokenExpiration = getEnvAsInt("AGORA_MIN_TOKEN_EXPIRATION", 60); // 1 minute min

        LOGGER.info("AgoraConfig initialized successfully");
        LOGGER.info("App ID: " + maskAppId(appId));
        LOGGER.info("Default Token Expiration: " + defaultTokenExpiration + " seconds");
        LOGGER.info("Max Token Expiration: " + maxTokenExpiration + " seconds");
        LOGGER.info("Min Token Expiration: " + minTokenExpiration + " seconds");
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

    /**
     * Mask App ID for logging (show only first 4 and last 4 characters)
     */
    private String maskAppId(String appId) {
        if (appId == null || appId.length() <= 8) {
            return "****";
        }
        return appId.substring(0, 4) + "****" + appId.substring(appId.length() - 4);
    }
}
