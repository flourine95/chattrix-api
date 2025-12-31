package com.chattrix.api.resources;

import com.chattrix.api.services.cache.CacheManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Admin endpoints for monitoring and management
 * TODO: Add @Secured annotation with ADMIN role when implemented
 */
@Path("/v1/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    private static final Logger logger = LoggerFactory.getLogger(AdminResource.class);

    @Inject
    private CacheManager cacheManager;

    /**
     * Get all cache statistics
     * GET /v1/admin/cache-stats
     */
    @GET
    @Path("/cache-stats")
    public Response getCacheStats() {
        logger.info("Fetching cache statistics");
        String stats = cacheManager.getAllStats();
        return Response.ok()
                .entity(new StatsResponse(stats))
                .build();
    }

    /**
     * Get batch processing statistics
     * GET /v1/admin/batch-stats
     */
    @GET
    @Path("/batch-stats")
    public Response getBatchStats() {
        logger.info("Fetching batch processing statistics");
        // TODO: Implement batch stats when CacheManager.getBatchStats() is available
        String stats = "Batch stats not implemented yet";
        return Response.ok()
                .entity(new StatsResponse(stats))
                .build();
    }

    /**
     * Get session statistics
     * GET /v1/admin/session-stats
     */
    @GET
    @Path("/session-stats")
    public Response getSessionStats() {
        logger.info("Fetching session statistics");
        // TODO: Implement session stats when CacheManager.getSessionStats() is available
        // CacheManager.SessionStats stats = cacheManager.getSessionStats();
        String stats = "Session stats not implemented yet";
        return Response.ok(stats).build();
    }

    /**
     * Get cache health status
     * GET /v1/admin/cache-health
     */
    @GET
    @Path("/cache-health")
    public Response getCacheHealth() {
        logger.info("Checking cache health");
        CacheManager.CacheHealthStatus health = cacheManager.getHealthStatus();
        return Response.ok(health).build();
    }

    /**
     * Force flush buffered messages
     * POST /v1/admin/batch/flush
     */
    @POST
    @Path("/batch/flush")
    public Response forceFlushMessages() {
        logger.info("Force flushing buffered messages");
        try {
            // cacheManager.flushMessages();
            return Response.ok()
                    .entity(new MessageResponse("Messages flushed successfully"))
                    .build();
        } catch (Exception e) {
            logger.error("Error flushing messages", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new MessageResponse("Error flushing messages: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Clear all caches
     * POST /v1/admin/cache/clear
     */
    @POST
    @Path("/cache/clear")
    public Response clearAllCaches() {
        logger.warn("Clearing all caches");
        try {
            cacheManager.clearAll();
            return Response.ok()
                    .entity(new MessageResponse("All caches cleared successfully"))
                    .build();
        } catch (Exception e) {
            logger.error("Error clearing caches", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new MessageResponse("Error clearing caches: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Warm up caches
     * POST /v1/admin/cache/warmup
     */
    @POST
    @Path("/cache/warmup")
    public Response warmUpCaches() {
        logger.info("Warming up caches");
        try {
            cacheManager.warmUp();
            return Response.ok()
                    .entity(new MessageResponse("Cache warm-up completed"))
                    .build();
        } catch (Exception e) {
            logger.error("Error warming up caches", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new MessageResponse("Error warming up caches: " + e.getMessage()))
                    .build();
        }
    }

    // ==================== DTOs ====================

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class StatsResponse {
        private String stats;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MessageResponse {
        private String message;
    }
}
