package com.chattrix.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

import java.util.logging.Logger;

/**
 * Configuration class for Agora RTC SDK settings.
 * Loads Agora App ID, App Certificate, and token expiration settings.
 */
@ApplicationScoped
@Getter
public class AgoraConfig {

    private static final Logger LOGGER = Logger.getLogger(AgoraConfig.class.getName());

    @Inject
    private AppConfig appConfig;

    private String appId;
    private String appCertificate;
    private int defaultTokenExpiration;
    private int maxTokenExpiration;
    private int minTokenExpiration;

    @PostConstruct
    public void init() {
        // Load Agora App ID (Required)
        appId = appConfig.getRequired("agora.app.id");

        // Load Agora App Certificate (Required)
        appCertificate = appConfig.getRequired("agora.app.certificate");

        // Load token expiration settings with defaults
        defaultTokenExpiration = appConfig.getInt("agora.token.expiration.default", 3600);
        maxTokenExpiration = appConfig.getInt("agora.token.expiration.max", 86400);
        minTokenExpiration = appConfig.getInt("agora.token.expiration.min", 60);

        LOGGER.info("AgoraConfig initialized successfully");
        LOGGER.info("App ID: " + AppConfig.maskSensitive(appId));
        LOGGER.info("Default Token Expiration: " + defaultTokenExpiration + " seconds");
        LOGGER.info("Max Token Expiration: " + maxTokenExpiration + " seconds");
        LOGGER.info("Min Token Expiration: " + minTokenExpiration + " seconds");
    }
}
