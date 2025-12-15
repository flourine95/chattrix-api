package com.chattrix.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@ApplicationScoped
@Getter
@Slf4j
public class AppConfig {

    private Properties properties;

    public static String maskSensitive(String value) {
        if (value == null || value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    @PostConstruct
    public void init() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
                log.info("Loaded application.properties successfully");
            } else {
                log.warn("application.properties not found, using environment variables only");
            }
        } catch (IOException e) {
            log.error("Failed to load application.properties", e);
            throw new RuntimeException("Failed to load application configuration", e);
        }
    }

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

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value for '{}': {}. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public long getLong(String key, long defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid long value for '{}': {}. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
}