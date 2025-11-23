package com.chattrix.api.websocket.handlers.call;

import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.exceptions.UnauthorizedException;
import com.chattrix.api.services.CallService;
import com.chattrix.api.websocket.WebSocketErrorHelper;
import com.chattrix.api.websocket.dto.CallRejectDto;
import com.chattrix.api.websocket.handlers.MessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for call reject messages.
 * Processes call rejection requests from WebSocket clients.
 * Validates: Requirements 1.1, 1.2, 1.4, 1.5, 4.3, 5.1, 5.2, 9.1, 9.2, 9.3
 */
@ApplicationScoped
public class CallRejectHandler implements MessageHandler {
    
    private static final Logger LOGGER = Logger.getLogger(CallRejectHandler.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Inject
    private CallService callService;
    
    @Override
    public void handle(Session session, Long userId, Object payload) {
        try {
            // Convert payload to DTO
            CallRejectDto callRejectDto = objectMapper.convertValue(payload, CallRejectDto.class);
            
            // Extract callId and reason from DTO
            String callId = callRejectDto.getCallId();
            String reason = callRejectDto.getReason();
            
            if (callId == null || callId.trim().isEmpty()) {
                WebSocketErrorHelper.sendError(session, null, "invalid_request", "Call ID is required");
                return;
            }
            
            LOGGER.log(Level.INFO, "Processing call reject for call: {0} by user: {1} with reason: {2}", 
                new Object[]{callId, userId, reason});
            
            // Invoke CallService to process the rejection
            callService.rejectCallViaWebSocket(callId, String.valueOf(userId), reason);
            
            LOGGER.log(Level.INFO, "Call rejected successfully: {0}", callId);
            
        } catch (ResourceNotFoundException e) {
            // Call not found
            String callId = extractCallIdFromPayload(payload);
            WebSocketErrorHelper.sendError(session, callId, "call_not_found", e.getMessage());
            LOGGER.log(Level.WARNING, "Call not found: {0}", e.getMessage());
        } catch (UnauthorizedException e) {
            // User is not authorized (not the callee)
            String callId = extractCallIdFromPayload(payload);
            WebSocketErrorHelper.sendError(session, callId, "unauthorized", e.getMessage());
            LOGGER.log(Level.WARNING, "Unauthorized call reject: {0}", e.getMessage());
        } catch (BadRequestException e) {
            // Invalid call status
            String callId = extractCallIdFromPayload(payload);
            WebSocketErrorHelper.sendError(session, callId, "invalid_status", e.getMessage());
            LOGGER.log(Level.WARNING, "Invalid call status: {0}", e.getMessage());
        } catch (Exception e) {
            // Generic error
            String callId = extractCallIdFromPayload(payload);
            WebSocketErrorHelper.sendError(session, callId, "service_error", "An unexpected error occurred");
            LOGGER.log(Level.SEVERE, "Error processing call reject", e);
        }
    }
    
    @Override
    public String getMessageType() {
        return "call.reject";
    }
    
    /**
     * Helper method to extract callId from payload for error handling
     */
    private String extractCallIdFromPayload(Object payload) {
        try {
            if (payload instanceof Map) {
                Object callId = ((Map<?, ?>) payload).get("callId");
                return callId != null ? callId.toString() : null;
            }
            CallRejectDto dto = objectMapper.convertValue(payload, CallRejectDto.class);
            return dto.getCallId();
        } catch (Exception e) {
            return null;
        }
    }
}
