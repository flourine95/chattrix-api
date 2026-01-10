package com.chattrix.api.resources.admin;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.services.cache.CacheManager;
import com.chattrix.api.services.cache.UnreadCountSyncService;
import com.chattrix.api.services.message.MessageBatchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin endpoints for cache management and monitoring
 * 
 * Endpoints:
 * - GET /api/admin/cache/stats - Get cache statistics
 * - GET /api/admin/cache/health - Get cache health status
 * - GET /api/admin/cache/metrics - Get detailed metrics
 * - POST /api/admin/cache/clear - Clear all caches
 * - POST /api/admin/cache/warmup - Warm up caches
 * - POST /api/admin/sync/unread-counts - Force sync unread counts
 * - POST /api/admin/sync/messages - Force flush message buffer
 * - GET /api/admin/buffer/stats - Get message buffer statistics
 */
@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class CacheAdminResource {

    @Inject
    private CacheManager cacheManager;
    
    @Inject
    private UnreadCountSyncService unreadCountSyncService;
    
    @Inject
    private MessageBatchService messageBatchService;

    // ==================== CACHE MANAGEMENT ====================

    /**
     * Get cache statistics (text format for easy reading)
     */
    @GET
    @Path("/cache/stats")
    @Secured
    @Produces(MediaType.TEXT_PLAIN)
    public Response getCacheStats() {
        log.info("Admin: Getting cache statistics");
        String stats = cacheManager.getAllStats();
        return Response.ok(stats).build();
    }

    /**
     * Get cache health status (JSON format for monitoring)
     */
    @GET
    @Path("/cache/health")
    @Secured
    public Response getCacheHealth() {
        log.info("Admin: Getting cache health status");
        CacheManager.CacheHealthStatus health = cacheManager.getHealthStatus();
        return Response.ok(health).build();
    }

    /**
     * Get detailed cache metrics (for Prometheus, Grafana, etc.)
     */
    @GET
    @Path("/cache/metrics")
    @Secured
    public Response getCacheMetrics() {
        log.info("Admin: Getting detailed cache metrics");
        Map<String, CacheManager.CacheMetrics> metrics = cacheManager.getDetailedMetrics();
        return Response.ok(metrics).build();
    }

    /**
     * Get cache efficiency report with recommendations
     */
    @GET
    @Path("/cache/efficiency")
    @Secured
    public Response getCacheEfficiency() {
        log.info("Admin: Getting cache efficiency report");
        CacheManager.CacheEfficiencyReport report = cacheManager.getEfficiencyReport();
        return Response.ok(report).build();
    }

    /**
     * Clear all caches (use with caution!)
     */
    @POST
    @Path("/cache/clear")
    @Secured
    public Response clearAllCaches() {
        log.warn("Admin: Clearing all caches - this will cause temporary performance degradation");
        cacheManager.clearAll();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "All caches cleared successfully");
        response.put("warning", "Cache miss storm expected - performance will recover gradually");
        
        return Response.ok(response).build();
    }

    /**
     * Warm up caches (pre-load frequently accessed data)
     */
    @POST
    @Path("/cache/warmup")
    @Secured
    public Response warmUpCaches() {
        log.info("Admin: Warming up caches");
        cacheManager.warmUp();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Cache warm-up completed");
        
        return Response.ok(response).build();
    }

    // ==================== SYNC OPERATIONS ====================

    /**
     * Force sync unread counts from cache to database
     */
    @POST
    @Path("/sync/unread-counts")
    @Secured
    public Response forceSyncUnreadCounts() {
        log.info("Admin: Force syncing unread counts");
        
        try {
            unreadCountSyncService.syncToDatabase();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Unread counts synced to database");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            log.error("Failed to sync unread counts", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to sync unread counts: " + e.getMessage());
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(response)
                    .build();
        }
    }

    /**
     * Force flush message buffer to database
     */
    @POST
    @Path("/sync/messages")
    @Secured
    public Response forceFlushMessages() {
        log.info("Admin: Force flushing message buffer");
        
        try {
            messageBatchService.forceFlush();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Message buffer flushed to database");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            log.error("Failed to flush message buffer", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to flush messages: " + e.getMessage());
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(response)
                    .build();
        }
    }

    // ==================== BUFFER STATISTICS ====================

    /**
     * Get message buffer statistics
     */
    @GET
    @Path("/buffer/stats")
    @Secured
    public Response getBufferStats() {
        log.info("Admin: Getting message buffer statistics");
        MessageBatchService.BufferStats stats = messageBatchService.getStats();
        return Response.ok(stats).build();
    }

    /**
     * Clear message buffer (for testing only!)
     */
    @POST
    @Path("/buffer/clear")
    @Secured
    public Response clearMessageBuffer() {
        log.warn("Admin: Clearing message buffer - messages will be lost!");
        messageBatchService.clearBuffer();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Message buffer cleared");
        response.put("warning", "Buffered messages were discarded!");
        
        return Response.ok(response).build();
    }

    // ==================== SYSTEM INFO ====================

    /**
     * Get overall system health (cache + buffer + sync)
     */
    @GET
    @Path("/health")
    @Secured
    public Response getSystemHealth() {
        log.info("Admin: Getting overall system health");
        
        Map<String, Object> health = new HashMap<>();
        health.put("cache", cacheManager.getHealthStatus());
        health.put("messageBuffer", messageBatchService.getStats());
        health.put("timestamp", java.time.Instant.now());
        
        return Response.ok(health).build();
    }
}
