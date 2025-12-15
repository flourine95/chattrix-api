package com.chattrix.api.websocket.handlers;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.util.logging.Logger;

/**
 * Startup bean to eagerly initialize the MessageHandlerRegistry.
 * <p>
 * This bean ensures that all WebSocket message handlers are discovered
 * and registered during application startup, rather than waiting for
 * the first WebSocket connection.
 * </p>
 * <p>
 * The {@code @Startup} annotation ensures this bean is instantiated
 * when the application starts, and the {@code @PostConstruct} method
 * triggers the registry initialization by simply accessing it.
 * </p>
 */
@Startup
@Singleton
public class HandlerRegistryInitializer {

    private static final Logger LOGGER = Logger.getLogger(HandlerRegistryInitializer.class.getName());

    @Inject
    private MessageHandlerRegistry registry;

    @PostConstruct
    public void initialize() {
        LOGGER.info("Triggering MessageHandlerRegistry initialization...");
        // Simply accessing the registry triggers its @PostConstruct initialization
        int handlerCount = registry.getHandlerCount();
        LOGGER.info("MessageHandlerRegistry initialized with " + handlerCount + " handlers");
    }
}
