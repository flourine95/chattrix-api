package com.chattrix.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Getter
@Slf4j
public class AgoraConfig {

    @Inject
    private AppConfig appConfig;

    private String appId;
    private String appCertificate;
    private int defaultTokenExpiration;
    private int maxTokenExpiration;
    private int minTokenExpiration;

    @PostConstruct
    public void init() {
        appId = appConfig.getRequired("agora.app.id");
        appCertificate = appConfig.getRequired("agora.app.certificate");
        defaultTokenExpiration = appConfig.getInt("agora.token.expiration.default", 3600);
        maxTokenExpiration = appConfig.getInt("agora.token.expiration.max", 86400);
        minTokenExpiration = appConfig.getInt("agora.token.expiration.min", 60);

        log.info("AgoraConfig initialized successfully");
        log.info("App ID: {}", AppConfig.maskSensitive(appId));
        log.info("Default Token Expiration: {} seconds", defaultTokenExpiration);
        log.info("Max Token Expiration: {} seconds", maxTokenExpiration);
        log.info("Min Token Expiration: {} seconds", minTokenExpiration);
    }
}