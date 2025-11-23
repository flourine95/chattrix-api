package com.chattrix.api.websocket;

import com.chattrix.api.websocket.dto.CallErrorDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.websocket.Session;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for handling WebSocket error responses.
 * Provides consistent error handling across all WebSocket message handlers.
 * 
 * <p>This helper ensures that error responses follow a uniform format and
 * are properly logged. It is designed to be used by call-related handlers
 * to send standardized error messages to WebSocket clients.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>
 * try {
 *     // Process message
 * } catch (ResourceNotFoundException e) {
 *     WebSocketErrorHelper.sendError(session, callId, "call_not_found", e.getMessage());
 * }
 * </pre>
 * 
 * <p><b>Error Types:</b></p>
 * <ul>
 *   <li><code>invalid_request</code> - Missing or invalid required parameters</li>
 *   <li><code>call_not_found</code> - The specified call does not exist</li>
 *   <li><code>unauthorized</code> - User is not authorized to perform the action</li>
 *   <li><code>invalid_status</code> - Call is in an invalid state for the requested operation</li>
 *   <li><code>service_error</code> - Unexpected server error occurred</li>
 * </ul>
 * 
 * @see CallErrorDto
 * @see WebSocketMessage
 * 
 * Validates: Requirements 9.1, 9.2, 9.3, 9.4
 */
public class WebSocketErrorHelper {
    
    private static final Logger LOGGER = Logger.getLogger(WebSocketErrorHelper.class.getName());
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private WebSocketErrorHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Sends a standardized error message to the WebSocket client.
     * 
     * <p>This method creates a {@link CallErrorDto} with the provided error details,
     * wraps it in a {@link WebSocketMessage}, and sends it to the client through
     * the provided session. All errors are logged for monitoring and debugging purposes.</p>
     * 
     * <p>If sending the error message fails (e.g., due to a closed connection),
     * the failure is logged but no exception is thrown to prevent cascading errors.</p>
     * 
     * @param session the WebSocket session to send the error message through
     * @param callId the ID of the call related to the error, or null if not applicable
     * @param errorType the type of error (e.g., "call_not_found", "unauthorized")
     * @param message a human-readable error message describing what went wrong
     * 
     * @throws NullPointerException if session, errorType, or message is null
     * 
     * Validates: Requirements 9.1, 9.2, 9.3
     */
    public static void sendError(Session session, String callId, String errorType, String message) {
        if (session == null) {
            LOGGER.log(Level.SEVERE, "Cannot send error: session is null");
            return;
        }
        
        if (errorType == null || message == null) {
            LOGGER.log(Level.SEVERE, "Cannot send error: errorType or message is null");
            return;
        }
        
        try {
            // Create error DTO
            CallErrorDto errorDto = new CallErrorDto();
            errorDto.setCallId(callId);
            errorDto.setErrorType(errorType);
            errorDto.setMessage(message);
            
            // Wrap in WebSocket message
            WebSocketMessage<CallErrorDto> wsMessage = 
                new WebSocketMessage<>("call_error", errorDto);
            
            // Send to client
            session.getBasicRemote().sendObject(wsMessage);
            
            // Log the error for monitoring
            LOGGER.log(Level.INFO, "Sent call error to client - Type: {0}, Message: {1}, CallId: {2}", 
                new Object[]{errorType, message, callId});
            
        } catch (Exception e) {
            // Log failure but don't throw to prevent cascading errors
            LOGGER.log(Level.SEVERE, "Failed to send error message to client - Type: " + errorType + ", Message: " + message, e);
        }
    }
}
