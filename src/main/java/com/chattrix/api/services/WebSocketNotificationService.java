package com.chattrix.api.services;

import com.chattrix.api.websocket.dto.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@NoArgsConstructor(force = true)
@Slf4j
public class WebSocketNotificationService {

    private final ChatSessionService chatSessionService;

    public void sendCallInvitation(String calleeId, CallInvitationDto data) {
        try {
            Long calleeIdLong = Long.parseLong(calleeId);

            WebSocketMessage<CallInvitationDto> message = new WebSocketMessage<>("call.incoming", data);

            chatSessionService.sendDirectMessage(calleeIdLong, message);

            log.info("Sent call invitation to user {} for call {}", calleeId, data.getCallId());
        } catch (NumberFormatException e) {
            log.error("Invalid callee ID format: {}", calleeId, e);
        } catch (Exception e) {
            log.error("Failed to send call invitation to user {}", calleeId, e);
        }
    }

    /**
     * Send call accepted notification to the caller
     * Message Type: "call.accepted"
     */
    public void sendCallAccepted(String callerId, String callId, String acceptedBy) {
        try {
            Long callerIdLong = Long.parseLong(callerId);

            CallAcceptDto data = CallAcceptDto.builder()
                    .callId(callId)
                    .acceptedBy(Long.parseLong(acceptedBy))
                    .build();

            WebSocketMessage<CallAcceptDto> message = new WebSocketMessage<>("call.accepted", data);

            chatSessionService.sendDirectMessage(callerIdLong, message);

            log.info("Sent call accepted notification to user {} for call {}", callerId, callId);
        } catch (NumberFormatException e) {
            log.error("Invalid ID format in sendCallAccepted: caller={}, acceptedBy={}", callerId, acceptedBy, e);
        } catch (Exception e) {
            log.error("Failed to send call accepted notification to user {}", callerId, e);
        }
    }

    /**
     * Send call rejected notification to the caller
     * Message Type: "call.rejected"
     */
    public void sendCallRejected(String callerId, String callId, String rejectedBy, String reason) {
        try {
            Long callerIdLong = Long.parseLong(callerId);

            CallRejectDto data = CallRejectDto.builder()
                    .callId(callId)
                    .rejectedBy(Long.parseLong(rejectedBy))
                    .reason(reason)
                    .build();

            WebSocketMessage<CallRejectDto> message = new WebSocketMessage<>("call.rejected", data);

            chatSessionService.sendDirectMessage(callerIdLong, message);

            log.info("Sent call rejected notification to user {} for call {} with reason: {}", callerId, callId, reason);
        } catch (NumberFormatException e) {
            log.error("Invalid ID format in sendCallRejected", e);
        } catch (Exception e) {
            log.error("Failed to send call rejected notification to user {}", callerId, e);
        }
    }

    /**
     * Send call ended notification to the other participant
     * Message Type: "call.ended"
     */
    public void sendCallEnded(String userId, String callId, String endedBy, Integer durationSeconds) {
        try {
            Long userIdLong = Long.parseLong(userId);

            CallEndDto data = CallEndDto.builder()
                    .callId(callId)
                    .endedBy(Long.parseLong(endedBy))
                    .durationSeconds(durationSeconds)
                    .build();

            WebSocketMessage<CallEndDto> message = new WebSocketMessage<>("call.ended", data);

            chatSessionService.sendDirectMessage(userIdLong, message);

            log.info("Sent call ended notification to user {} for call {}", userId, callId);
        } catch (NumberFormatException e) {
            log.error("Invalid ID format in sendCallEnded", e);
        } catch (Exception e) {
            log.error("Failed to send call ended notification to user {}", userId, e);
        }
    }

    /**
     * Send call timeout notification to both participants
     * Message Type: "call.timeout"
     */
    public void sendCallTimeout(String callerId, String calleeId, String callId) {
        try {
            CallTimeoutDto data = CallTimeoutDto.builder()
                    .callId(callId)
                    .reason("no_answer")
                    .build();

            WebSocketMessage<CallTimeoutDto> message = new WebSocketMessage<>("call.timeout", data);

            // Gửi cho Caller
            try {
                chatSessionService.sendDirectMessage(Long.parseLong(callerId), message);
            } catch (Exception e) {
                log.warn("Could not send timeout to caller {}", callerId);
            }

            // Gửi cho Callee
            try {
                chatSessionService.sendDirectMessage(Long.parseLong(calleeId), message);
            } catch (Exception e) {
                log.warn("Could not send timeout to callee {}", calleeId);
            }

            log.info("Sent call timeout notifications for call {}", callId);

        } catch (Exception e) {
            log.error("Failed to send call timeout notification for call {}", callId, e);
        }
    }
}