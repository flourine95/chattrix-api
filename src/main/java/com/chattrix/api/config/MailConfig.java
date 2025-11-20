package com.chattrix.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

import java.util.logging.Logger;

/**
 * Configuration class for email/SMTP settings.
 * Loads Gmail SMTP configuration for sending emails.
 */
@ApplicationScoped
@Getter
public class MailConfig {

    private static final Logger LOGGER = Logger.getLogger(MailConfig.class.getName());

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
        // Load SMTP settings (default to Gmail SMTP)
        smtpHost = appConfig.get("mail.smtp.host");
        if (smtpHost == null || smtpHost.isEmpty()) {
            smtpHost = "smtp.gmail.com";
        }
        
        smtpPort = appConfig.getInt("mail.smtp.port", 587);
        starttlsEnable = appConfig.getBoolean("mail.smtp.starttls.enable", true);
        smtpAuth = appConfig.getBoolean("mail.smtp.auth", true);

        // Load credentials
        username = appConfig.get("mail.username");
        password = appConfig.get("mail.password");

        // Load from address settings
        fromAddress = appConfig.get("mail.from");
        if (fromAddress == null || fromAddress.isEmpty()) {
            fromAddress = "noreply@chattrix.com";
        }

        fromName = appConfig.get("mail.from.name");
        if (fromName == null || fromName.isEmpty()) {
            fromName = "Chattrix";
        }

        LOGGER.info("MailConfig initialized successfully");
        LOGGER.info("SMTP Host: " + smtpHost);
        LOGGER.info("SMTP Port: " + smtpPort);
        LOGGER.info("STARTTLS Enabled: " + starttlsEnable);
        LOGGER.info("SMTP Auth: " + smtpAuth);
        LOGGER.info("Username: " + (username != null ? AppConfig.maskSensitive(username) : "not set"));
        LOGGER.info("From Address: " + fromAddress);
        LOGGER.info("From Name: " + fromName);
    }

    /**
     * Check if email configuration is complete.
     */
    public boolean isConfigured() {
        return smtpHost != null && !smtpHost.isEmpty()
                && username != null && !username.isEmpty()
                && password != null && !password.isEmpty();
    }
}
