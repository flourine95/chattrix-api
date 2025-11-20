package com.chattrix.api.services;

import com.chattrix.api.websocket.dto.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for sending WebSocket notifications related to call events.
 * Integrates with ChatSessionService to deliver real-time notifications to users.
 */
@ApplicationScoped
public class WebSocketNotificationService {

    private static final Logger LOGGER = Logger.getLogger(WebSocketNotificationService.class.getName());

    @Inject
    private ChatSessionService chatSessionService;

    /**
     * Send call invitation notification to the callee
     * 
     * @param calleeId The ID of the user receiving the call invitation
     * @param data The call invitation data containing caller info and call details
     */
    public void sendCallInvitation(String calleeId, CallInvitationData data) {
        try {
            Long calleeIdLong = Long.parseLong(calleeId);
            
            CallInvitationMessage message = new CallInvitationMessage(data);
            WebSocketMessage<CallInvitationMessage> wsMessage = 
                new WebSocketMessage<>("call_invitation", message);
            
            chatSessionService.sendMessageToUser(calleeIdLong, wsMessage);
            
            LOGGER.log(Level.INFO, "Sent call invitation to user {0} for call {1}", 
                new Object[]{calleeId, data.getCallId()});
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid callee ID format: " + calleeId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send call invitation to user " + calleeId, e);
        }
    }

    /**
     * Send call accepted notification to the caller
     * 
     * @param callerId The ID of the user who initiated the call
     * @param callId The ID of the call that was accepted
     * @param acceptedBy The ID of the user who accepted the call
     */
    public void sendCallAccepted(String callerId, String callId, String acceptedBy) {
        try {
            Long callerIdLong = Long.parseLong(callerId);
            
            CallAcceptedData data = new CallAcceptedData();
            data.setCallId(callId);
            data.setAcceptedBy(acceptedBy);
            
            CallAcceptedMessage message = new CallAcceptedMessage();
            message.setType("call_accepted");
            message.setData(data);
            message.setTimestamp(Instant.now());
            
            WebSocketMessage<CallAcceptedMessage> wsMessage = 
                new WebSocketMessage<>("call_accepted", message);
            
            chatSessionService.sendMessageToUser(callerIdLong, wsMessage);
            
            LOGGER.log(Level.INFO, "Sent call accepted notification to user {0} for call {1}", 
                new Object[]{callerId, callId});
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid caller ID format: " + callerId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send call accepted notification to user " + callerId, e);
        }
    }

    /**
     * Send call rejected notification to the caller
     * 
     * @param callerId The ID of the user who initiated the call
     * @param callId The ID of the call that was rejected
     * @param rejectedBy The ID of the user who rejected the call
     * @param reason The reason for rejection (busy, declined, unavailable)
     */
    public void sendCallRejected(String callerId, String callId, String rejectedBy, String reason) {
        try {
            Long callerIdLong = Long.parseLong(callerId);
            
            CallRejectedData data = new CallRejectedData();
            data.setCallId(callId);
            data.setRejectedBy(rejectedBy);
            data.setReason(reason);
            
            CallRejectedMessage message = new CallRejectedMessage();
            message.setType("call_rejected");
            message.setData(data);
            message.setTimestamp(Instant.now());
            
            WebSocketMessage<CallRejectedMessage> wsMessage = 
                new WebSocketMessage<>("call_rejected", message);
            
            chatSessionService.sendMessageToUser(callerIdLong, wsMessage);
            
            LOGGER.log(Level.INFO, "Sent call rejected notification to user {0} for call {1} with reason: {2}", 
                new Object[]{callerId, callId, reason});
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid caller ID format: " + callerId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send call rejected notification to user " + callerId, e);
        }
    }

    /**
     * Send call ended notification to the other participant
     * 
     * @param userId The ID of the user to notify
     * @param callId The ID of the call that ended
     * @param endedBy The ID of the user who ended the call
     * @param durationSeconds The duration of the call in seconds
     */
    public void sendCallEnded(String userId, String callId, String endedBy, Integer durationSeconds) {
        try {
            Long userIdLong = Long.parseLong(userId);
            
            CallEndedData data = new CallEndedData();
            data.setCallId(callId);
            data.setEndedBy(endedBy);
            data.setDurationSeconds(durationSeconds);
            
            CallEndedMessage message = new CallEndedMessage();
            message.setType("call_ended");
            message.setData(data);
            message.setTimestamp(Instant.now());
            
            WebSocketMessage<CallEndedMessage> wsMessage = 
                new WebSocketMessage<>("call_ended", message);
            
            chatSessionService.sendMessageToUser(userIdLong, wsMessage);
            
            LOGGER.log(Level.INFO, "Sent call ended notification to user {0} for call {1}", 
                new Object[]{userId, callId});
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid user ID format: " + userId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send call ended notification to user " + userId, e);
        }
    }

    /**
     * Send call timeout notification to both participants
     * 
     * @param callerId The ID of the caller
     * @param calleeId The ID of the callee
     * @param callId The ID of the call that timed out
     */
    public void sendCallTimeout(String callerId, String calleeId, String callId) {
        try {
            CallTimeoutData data = new CallTimeoutData();
            data.setCallId(callId);
            
            CallTimeoutMessage message = new CallTimeoutMessage();
            message.setType("call_timeout");
            message.setData(data);
            message.setTimestamp(Instant.now());
            
            WebSocketMessage<CallTimeoutMessage> wsMessage = 
                new WebSocketMessage<>("call_timeout", message);
            
            // Send to caller
            try {
                Long callerIdLong = Long.parseLong(callerId);
                chatSessionService.sendMessageToUser(callerIdLong, wsMessage);
                LOGGER.log(Level.INFO, "Sent call timeout notification to caller {0} for call {1}", 
                    new Object[]{callerId, callId});
            } catch (NumberFormatException e) {
                LOGGER.log(Level.SEVERE, "Invalid caller ID format: " + callerId, e);
            }
            
            // Send to callee
            try {
                Long calleeIdLong = Long.parseLong(calleeId);
                chatSessionService.sendMessageToUser(calleeIdLong, wsMessage);
                LOGGER.log(Level.INFO, "Sent call timeout notification to callee {0} for call {1}", 
                    new Object[]{calleeId, callId});
            } catch (NumberFormatException e) {
                LOGGER.log(Level.SEVERE, "Invalid callee ID format: " + calleeId, e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send call timeout notification for call " + callId, e);
        }
    }

    /**
     * Send quality warning notification to the other participant
     * 
     * @param userId The ID of the user to notify
     * @param callId The ID of the call with quality issues
     * @param quality The network quality level (POOR, BAD, VERY_BAD)
     */
    public void sendQualityWarning(String userId, String callId, String quality) {
        try {
            Long userIdLong = Long.parseLong(userId);
            
            CallQualityWarningData data = new CallQualityWarningData();
            data.setCallId(callId);
            data.setQuality(quality);
            
            CallQualityWarningMessage message = new CallQualityWarningMessage();
            message.setType("call_quality_warning");
            message.setData(data);
            message.setTimestamp(Instant.now());
            
            WebSocketMessage<CallQualityWarningMessage> wsMessage = 
                new WebSocketMessage<>("call_quality_warning", message);
            
            chatSessionService.sendMessageToUser(userIdLong, wsMessage);
            
            LOGGER.log(Level.INFO, "Sent quality warning to user {0} for call {1} with quality: {2}", 
                new Object[]{userId, callId, quality});
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid user ID format: " + userId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send quality warning to user " + userId, e);
        }
    }
}
