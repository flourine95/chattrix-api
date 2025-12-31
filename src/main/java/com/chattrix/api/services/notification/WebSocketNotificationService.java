package com.chattrix.api.services.notification;

import com.chattrix.api.responses.FriendRequestResponse;
import com.chattrix.api.websocket.dto.*;
import com.chattrix.api.websocket.WebSocketEventType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@ApplicationScoped
@Slf4j
public class WebSocketNotificationService {

    @Inject
    private ChatSessionService chatSessionService;

    // ==================== Friend Request Events ====================

    public void sendFriendRequestReceived(Long receiverId, FriendRequestResponse friendRequest) {
        try {
            WebSocketMessage<FriendRequestResponse> message = new WebSocketMessage<>(WebSocketEventType.FRIEND_REQUEST_RECEIVED, friendRequest);
            chatSessionService.sendDirectMessage(receiverId, message);
        } catch (Exception e) {
            log.error("Failed to send friend request notification", e);
        }
    }

    public void sendFriendRequestAccepted(Long senderId, FriendRequestResponse friendRequest) {
        try {
            WebSocketMessage<FriendRequestResponse> message = new WebSocketMessage<>(WebSocketEventType.FRIEND_REQUEST_ACCEPTED, friendRequest);
            chatSessionService.sendDirectMessage(senderId, message);
        } catch (Exception e) {
            log.error("Failed to send friend request accepted notification", e);
        }
    }

    public void sendFriendRequestRejected(Long targetUserId, Long requestId, Long rejectedBy) {
        try {
            FriendRequestRejectEventDto payload = FriendRequestRejectEventDto.builder()
                    .requestId(requestId)
                    .rejectedBy(rejectedBy)
                    .build();
            WebSocketMessage<FriendRequestRejectEventDto> message = new WebSocketMessage<>(WebSocketEventType.FRIEND_REQUEST_REJECTED, payload);
            chatSessionService.sendDirectMessage(targetUserId, message);
        } catch (Exception e) {
            log.error("Failed to send friend request rejected notification", e);
        }
    }

    public void sendFriendRequestCancelled(Long targetUserId, Long requestId, Long cancelledBy) {
        try {
            FriendRequestCancelEventDto payload = FriendRequestCancelEventDto.builder()
                    .requestId(requestId)
                    .cancelledBy(cancelledBy)
                    .build();
            WebSocketMessage<FriendRequestCancelEventDto> message = new WebSocketMessage<>(WebSocketEventType.FRIEND_REQUEST_CANCELLED, payload);
            chatSessionService.sendDirectMessage(targetUserId, message);
        } catch (Exception e) {
            log.error("Failed to send friend request cancelled notification", e);
        }
    }

    // ==================== Call Events ====================

    public void sendCallInvitation(String calleeId, CallInvitationDto data) {
        try {
            WebSocketMessage<CallInvitationDto> message = new WebSocketMessage<>(WebSocketEventType.CALL_INCOMING, data);
            chatSessionService.sendDirectMessage(Long.parseLong(calleeId), message);
        } catch (Exception e) {
            log.error("Failed to send call invitation", e);
        }
    }

    /**
     * New method for group call participant updates
     * Message Type: "call.participant_update"
     */
    public void sendCallParticipantUpdate(Long targetUserId, CallParticipantUpdateDto data) {
        try {
            WebSocketMessage<CallParticipantUpdateDto> message = new WebSocketMessage<>(WebSocketEventType.CALL_PARTICIPANT_UPDATE, data);
            chatSessionService.sendDirectMessage(targetUserId, message);
        } catch (Exception e) {
            log.error("Failed to send participant update to user {}", targetUserId, e);
        }
    }

    public void sendCallAccepted(String callerId, String callId, String acceptedBy) {
        try {
            CallAcceptDto data = CallAcceptDto.builder()
                    .callId(callId)
                    .acceptedBy(Long.parseLong(acceptedBy))
                    .build();
            WebSocketMessage<CallAcceptDto> message = new WebSocketMessage<>(WebSocketEventType.CALL_ACCEPTED, data);
            chatSessionService.sendDirectMessage(Long.parseLong(callerId), message);
        } catch (Exception e) {
            log.error("Failed to send call accepted notification", e);
        }
    }

    public void sendCallRejected(String callerId, String callId, String rejectedBy, String reason) {
        try {
            CallRejectDto data = CallRejectDto.builder()
                    .callId(callId)
                    .rejectedBy(Long.parseLong(rejectedBy))
                    .reason(reason)
                    .build();
            WebSocketMessage<CallRejectDto> message = new WebSocketMessage<>(WebSocketEventType.CALL_REJECTED, data);
            chatSessionService.sendDirectMessage(Long.parseLong(callerId), message);
        } catch (Exception e) {
            log.error("Failed to send call rejected notification", e);
        }
    }

    public void sendCallEnded(String userId, String callId, String endedBy, Integer durationSeconds) {
        try {
            CallEndDto data = CallEndDto.builder()
                    .callId(callId)
                    .endedBy(Long.parseLong(endedBy))
                    .durationSeconds(durationSeconds)
                    .build();
            WebSocketMessage<CallEndDto> message = new WebSocketMessage<>(WebSocketEventType.CALL_ENDED, data);
            chatSessionService.sendDirectMessage(Long.parseLong(userId), message);
        } catch (Exception e) {
            log.error("Failed to send call ended notification", e);
        }
    }

    public void sendCallTimeout(String callerId, String calleeId, String callId) {
        try {
            CallTimeoutDto data = CallTimeoutDto.builder()
                    .callId(callId)
                    .reason("no_answer")
                    .build();
            WebSocketMessage<CallTimeoutDto> message = new WebSocketMessage<>(WebSocketEventType.CALL_TIMEOUT, data);
            chatSessionService.sendDirectMessage(Long.parseLong(callerId), message);
            chatSessionService.sendDirectMessage(Long.parseLong(calleeId), message);
        } catch (Exception e) {
            log.error("Failed to send call timeout notification", e);
        }
    }
}
