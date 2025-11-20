package com.chattrix.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

import java.util.logging.Logger;

/**
 * Configuration class for database settings.
 * Note: Actual datasource is configured at WildFly server level via standalone.xml
 * This class documents and validates the expected configuration.
 */
@ApplicationScoped
@Getter
public class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());

    @Inject
    private AppConfig appConfig;

    private String url;
    private String username;
    private String password;
    private String jndiName;

    @PostConstruct
    public void init() {
        // Load database settings
        url = appConfig.get("db.url");
        if (url == null || url.isEmpty()) {
            url = "jdbc:postgresql://localhost:5432/chattrix";
        }

        username = appConfig.get("db.username");
        if (username == null || username.isEmpty()) {
            username = "postgres";
        }

        password = appConfig.get("db.password");
        
        jndiName = appConfig.get("db.jndi.name");
        if (jndiName == null || jndiName.isEmpty()) {
            jndiName = "java:/PostgresDS";
        }

        LOGGER.info("DatabaseConfig initialized successfully");
        LOGGER.info("Database URL: " + maskUrl(url));
        LOGGER.info("Database Username: " + username);
        LOGGER.info("JNDI Name: " + jndiName);
        
        if (password == null || password.isEmpty()) {
            LOGGER.warning("Database password is not set! Application may fail to connect.");
        }
    }

    /**
     * Mask database URL for logging (hide password if present in URL)
     */
    private String maskUrl(String url) {
        if (url == null) {
            return "not set";
        }
        // Mask password in URL like: jdbc:postgresql://user:password@host:port/db
        return url.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
    }

    /**
     * Get JDBC driver class name
     */
    public String getDriverClass() {
        return "org.postgresql.Driver";
    }

    /**
     * Check if database configuration is complete
     */
    public boolean isConfigured() {
        return url != null && !url.isEmpty()
                && username != null && !username.isEmpty()
                && password != null && !password.isEmpty();
    }
}
