package com.chattrix.api.resources.admin;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.services.notification.WebSocketEventHub;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin endpoint for monitoring WebSocket events
 */
@Path("/v1/admin/websocket")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
@Slf4j
public class WebSocketMetricsResource {
    
    @Inject
    private WebSocketEventHub eventHub;
    
    /**
     * Get WebSocket metrics
     * GET /v1/admin/websocket/metrics
     */
    @GET
    @Path("/metrics")
    public Response getMetrics() {
        WebSocketEventHub.WebSocketMetrics metrics = eventHub.getMetrics();
        return Response.ok(ApiResponse.success(metrics, "WebSocket metrics retrieved")).build();
    }
    
    /**
     * Get WebSocket metrics report (text format)
     * GET /v1/admin/websocket/metrics/report
     */
    @GET
    @Path("/metrics/report")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMetricsReport() {
        String report = eventHub.getMetricsReport();
        return Response.ok(report).build();
    }
    
    /**
     * Reset WebSocket metrics
     * POST /v1/admin/websocket/metrics/reset
     */
    @POST
    @Path("/metrics/reset")
    public Response resetMetrics() {
        eventHub.resetMetrics();
        return Response.ok(ApiResponse.success(null, "WebSocket metrics reset")).build();
    }
}
