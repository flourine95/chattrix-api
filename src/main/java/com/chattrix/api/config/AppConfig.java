package com.chattrix.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central configuration class for the Chattrix application.
 * Loads configuration from application.properties with fallback to system environment variables.
 * 
 * Priority: System Environment Variables > Properties File > Default Values
 */
@ApplicationScoped
@Getter
public class AppConfig {

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private Properties properties;

    @PostConstruct
    public void init() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
                LOGGER.info("Loaded application.properties successfully");
            } else {
                LOGGER.warning("application.properties not found, using environment variables only");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load application.properties", e);
            throw new RuntimeException("Failed to load application configuration", e);
        }
    }

    /**
     * Get configuration value with priority:
     * 1. System environment variable (uppercase with underscores)
     * 2. Properties file value (with ${ENV:default} resolution)
     * 3. null if not found
     */
    public String get(String key) {
        // Priority 1: Check system environment (convert dot notation to uppercase underscore)
        String envKey = key.toUpperCase().replace('.', '_');
        String value = System.getenv(envKey);
        
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // Priority 2: Check properties file
        value = properties.getProperty(key);
        
        // Resolve ${ENV_VAR:default} syntax
        if (value != null && value.startsWith("${") && value.endsWith("}")) {
            String placeholder = value.substring(2, value.length() - 1);
            String defaultValue = "";
            
            if (placeholder.contains(":")) {
                String[] parts = placeholder.split(":", 2);
                envKey = parts[0];
                defaultValue = parts[1];
            } else {
                envKey = placeholder;
            }
            
            value = System.getenv(envKey);
            if (value == null || value.isEmpty()) {
                value = defaultValue.isEmpty() ? null : defaultValue;
            }
        }
        
        return value;
    }

    /**
     * Get required configuration value. Throws exception if not found.
     */
    public String getRequired(String key) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(
                "Required configuration '" + key + "' is not set. " +
                "Set environment variable '" + key.toUpperCase().replace('.', '_') + "' or add to application.properties"
            );
        }
        return value;
    }

    /**
     * Get configuration value as integer with default.
     */
    public int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid integer value for '" + key + "': " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Get configuration value as boolean with default.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Get configuration value as long with default.
     */
    public long getLong(String key, long defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid long value for '" + key + "': " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Mask sensitive value for logging (show only first 4 and last 4 characters).
     */
    public static String maskSensitive(String value) {
        if (value == null || value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
