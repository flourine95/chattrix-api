package com.chattrix.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

import java.util.logging.Logger;

/**
 * Configuration class for JWT token settings.
 * Loads JWT secret and expiration settings.
 */
@ApplicationScoped
@Getter
public class JwtConfig {

    private static final Logger LOGGER = Logger.getLogger(JwtConfig.class.getName());

    @Inject
    private AppConfig appConfig;

    private String secret;
    private int accessExpirationMinutes;
    private int refreshExpirationDays;

    @PostConstruct
    public void init() {
        // Load JWT secret (Required)
        secret = appConfig.getRequired("jwt.secret");

        // Load expiration settings
        accessExpirationMinutes = appConfig.getInt("jwt.access.expiration.minutes", 15);
        refreshExpirationDays = appConfig.getInt("jwt.refresh.expiration.days", 7);

        LOGGER.info("JwtConfig initialized successfully");
        LOGGER.info("JWT Secret: " + AppConfig.maskSensitive(secret));
        LOGGER.info("Access Token Expiration: " + accessExpirationMinutes + " minutes");
        LOGGER.info("Refresh Token Expiration: " + refreshExpirationDays + " days");
    }

    /**
     * Get access token expiration in milliseconds.
     */
    public long getAccessExpirationMillis() {
        return accessExpirationMinutes * 60L * 1000L;
    }

    /**
     * Get refresh token expiration in milliseconds.
     */
    public long getRefreshExpirationMillis() {
        return refreshExpirationDays * 24L * 60L * 60L * 1000L;
    }
}
