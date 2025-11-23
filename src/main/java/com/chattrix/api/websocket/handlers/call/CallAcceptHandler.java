package com.chattrix.api.websocket.handlers.call;

import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.exceptions.UnauthorizedException;
import com.chattrix.api.services.CallService;
import com.chattrix.api.websocket.WebSocketErrorHelper;
import com.chattrix.api.websocket.dto.CallAcceptDto;
import com.chattrix.api.websocket.handlers.MessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CallAcceptHandler implements MessageHandler {
    
    private static final Logger LOGGER = Logger.getLogger(CallAcceptHandler.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Inject
    private CallService callService;
    
    @Override
    public void handle(Session session, Long userId, Object payload) {
        try {
            CallAcceptDto callAcceptDto = objectMapper.convertValue(payload, CallAcceptDto.class);
            
            String callId = callAcceptDto.getCallId();
            
            if (callId == null || callId.trim().isEmpty()) {
                WebSocketErrorHelper.sendError(session, null, "invalid_request", "Call ID is required");
                return;
            }
            
            LOGGER.log(Level.INFO, "Processing call accept for call: {0} by user: {1}", 
                new Object[]{callId, userId});
            
            callService.acceptCallViaWebSocket(callId, String.valueOf(userId));
            
            LOGGER.log(Level.INFO, "Call accepted successfully: {0}", callId);
            
        } catch (ResourceNotFoundException e) {
            String callId = extractCallIdFromPayload(payload);
            WebSocketErrorHelper.sendError(session, callId, "call_not_found", e.getMessage());
            LOGGER.log(Level.WARNING, "Call not found: {0}", e.getMessage());
        } catch (UnauthorizedException e) {
            String callId = extractCallIdFromPayload(payload);
            WebSocketErrorHelper.sendError(session, callId, "unauthorized", e.getMessage());
            LOGGER.log(Level.WARNING, "Unauthorized call accept: {0}", e.getMessage());
        } catch (BadRequestException e) {
            String callId = extractCallIdFromPayload(payload);
            WebSocketErrorHelper.sendError(session, callId, "invalid_status", e.getMessage());
            LOGGER.log(Level.WARNING, "Invalid call status: {0}", e.getMessage());
        } catch (Exception e) {
            String callId = extractCallIdFromPayload(payload);
            WebSocketErrorHelper.sendError(session, callId, "service_error", "An unexpected error occurred");
            LOGGER.log(Level.SEVERE, "Error processing call accept", e);
        }
    }
    
    @Override
    public String getMessageType() {
        return "call.accept";
    }
    
    private String extractCallIdFromPayload(Object payload) {
        try {
            if (payload instanceof Map) {
                Object callId = ((Map<?, ?>) payload).get("callId");
                return callId != null ? callId.toString() : null;
            }
            CallAcceptDto dto = objectMapper.convertValue(payload, CallAcceptDto.class);
            return dto.getCallId();
        } catch (Exception e) {
            return null;
        }
    }
}
