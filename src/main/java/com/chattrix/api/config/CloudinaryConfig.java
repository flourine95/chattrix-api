package com.chattrix.api.config;

import com.cloudinary.Cloudinary;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Getter
@Slf4j
public class CloudinaryConfig {

    @Inject
    private AppConfig appConfig;

    private String cloudinaryUrl;
    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        cloudinaryUrl = appConfig.getRequired("cloudinary.url");

        cloudinary = new Cloudinary(cloudinaryUrl);
        cloudinary.config.secure = true;

        log.info("CloudinaryConfig initialized successfully");
        log.info("Cloudinary URL: {}", AppConfig.maskSensitive(cloudinaryUrl));
        log.info("Secure mode: enabled");
    }

    @Produces
    @ApplicationScoped
    public Cloudinary cloudinary() {
        return cloudinary;
    }
}

