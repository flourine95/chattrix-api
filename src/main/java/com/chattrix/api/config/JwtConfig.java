package com.chattrix.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Getter
@Slf4j
public class JwtConfig {

    @Inject
    private AppConfig appConfig;

    private String secret;
    private int accessExpirationMinutes;
    private int refreshExpirationDays;

    @PostConstruct
    public void init() {
        secret = appConfig.getRequired("jwt.secret");

        accessExpirationMinutes = appConfig.getInt("jwt.access.expiration.minutes", 15);
        refreshExpirationDays = appConfig.getInt("jwt.refresh.expiration.days", 7);

        log.info("JwtConfig initialized successfully");
        log.info("JWT Secret: {}", AppConfig.maskSensitive(secret));
        log.info("Access Token Expiration: {} minutes", accessExpirationMinutes);
        log.info("Refresh Token Expiration: {} days", refreshExpirationDays);
    }

    public long getAccessExpirationMillis() {
        return accessExpirationMinutes * 60L * 1000L;
    }

    public long getRefreshExpirationMillis() {
        return refreshExpirationDays * 24L * 60L * 60L * 1000L;
    }
}