package com.chattrix.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Getter
@Slf4j
public class MailConfig {

    @Inject
    private AppConfig appConfig;

    private String smtpHost;
    private int smtpPort;
    private boolean starttlsEnable;
    private boolean smtpAuth;
    private String username;
    private String password;
    private String fromAddress;
    private String fromName;

    @PostConstruct
    public void init() {
        smtpHost = appConfig.get("mail.smtp.host");
        smtpPort = appConfig.getInt("mail.smtp.port", 587);
        starttlsEnable = appConfig.getBoolean("mail.smtp.starttls.enable", true);
        smtpAuth = appConfig.getBoolean("mail.smtp.auth", true);
        username = appConfig.get("mail.username");
        password = appConfig.get("mail.password");
        fromAddress = appConfig.get("mail.from");
        fromName = appConfig.get("mail.from.name");
        log.info("MailConfig initialized successfully");
        log.info("SMTP Host: {}", smtpHost);
        log.info("SMTP Port: {}", smtpPort);
        log.info("STARTTLS Enabled: {}", starttlsEnable);
        log.info("SMTP Auth: {}", smtpAuth);
        log.info("Username: {}", username != null ? AppConfig.maskSensitive(username) : "not set");
        log.info("From Address: {}", fromAddress);
        log.info("From Name: {}", fromName);
    }

    public boolean isConfigured() {
        return smtpHost != null && !smtpHost.isEmpty()
                && username != null && !username.isEmpty()
                && password != null && !password.isEmpty();
    }
}