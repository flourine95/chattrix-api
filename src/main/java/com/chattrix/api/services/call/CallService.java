package com.chattrix.api.services.call;

import com.chattrix.api.config.AgoraConfig;
import com.chattrix.api.entities.*;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.CallMapper;
import com.chattrix.api.repositories.CallRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.EndCallRequest;
import com.chattrix.api.requests.InitiateCallRequest;
import com.chattrix.api.requests.RejectCallRequest;
import com.chattrix.api.responses.CallConnectionResponse;
import com.chattrix.api.responses.CallResponse;
import com.chattrix.api.services.notification.WebSocketNotificationService;
import com.chattrix.api.websocket.dto.CallInvitationDto;
import com.chattrix.api.websocket.dto.CallParticipantUpdateDto;
import io.agora.media.RtcTokenBuilder;
import io.agora.media.RtcTokenBuilder.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@Transactional
public class CallService {

    @Inject
    private CallRepository callRepository;
    @Inject
    private UserRepository userRepository;
    @Inject
    private ConversationRepository conversationRepository;
    @Inject
    private CallMapper callMapper;
    @Inject
    private AgoraConfig agoraConfig;
    @Inject
    private WebSocketNotificationService webSocketService;
    @Inject
    private CallTimeoutScheduler timeoutScheduler;

    public CallConnectionResponse initiateCall(Long callerId, InitiateCallRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(request.getConversationId())
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        validateUserBusy(callerId);

        String channelId = String.format("channel_%d_%d", System.currentTimeMillis(), request.getConversationId());

        Call call = Call.builder()
                .id(UUID.randomUUID().toString())
                .channelId(channelId)
                .callerId(callerId)
                .conversationId(request.getConversationId())
                .callType(request.getCallType())
                .status(CallStatus.RINGING)
                .startTime(Instant.now())
                .build();

        List<CallParticipant> participants = new ArrayList<>();
        
        participants.add(CallParticipant.builder()
                .call(call)
                .userId(callerId)
                .status(ParticipantStatus.JOINED)
                .joinedAt(Instant.now())
                .build());

        List<CallParticipant> invitees = conversation.getParticipants().stream()
                .filter(p -> !p.getUser().getId().equals(callerId))
                .map(p -> CallParticipant.builder()
                        .call(call)
                        .userId(p.getUser().getId())
                        .status(ParticipantStatus.RINGING)
                        .build())
                .collect(Collectors.toList());
        
        participants.addAll(invitees);
        call.setParticipants(participants);
        
        Call savedCall = callRepository.save(call);
        User caller = findUser(callerId);
        
        for (CallParticipant p : invitees) {
            timeoutScheduler.scheduleTimeout(savedCall.getId(), callerId.toString(), p.getUserId().toString());
            sendInvitation(savedCall, caller, p.getUserId());
        }

        String token = generateAgoraToken(channelId, callerId.toString());
        return callMapper.toConnectionResponse(toFullResponse(savedCall), token);
    }

    public CallConnectionResponse acceptCall(String callId, Long userId) {
        Call call = findCall(callId);

        if (call.isFinished()) {
            throw BusinessException.badRequest("Call already ended", "CALL_ALREADY_ENDED");
        }

        CallParticipant participant = call.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseGet(() -> {
                    CallParticipant newP = CallParticipant.builder()
                            .call(call)
                            .userId(userId)
                            .build();
                    call.getParticipants().add(newP);
                    return newP;
                });

        participant.setStatus(ParticipantStatus.JOINED);
        participant.setJoinedAt(Instant.now());
        
        if (call.getStatus() == CallStatus.RINGING) {
            call.setStatus(CallStatus.CONNECTED);
        }

        timeoutScheduler.cancelTimeout(callId);
        Call savedCall = callRepository.save(call);

        notifyParticipantUpdate(savedCall, userId, ParticipantStatus.JOINED);

        String token = generateAgoraToken(savedCall.getChannelId(), userId.toString());
        return callMapper.toConnectionResponse(toFullResponse(savedCall), token);
    }

    public CallResponse getActiveCall(Long conversationId) {
        return callRepository.findActiveCallByConversationId(conversationId)
                .map(this::toFullResponse)
                .orElse(null);
    }

    public CallResponse rejectCall(String callId, Long userId, RejectCallRequest request) {
        Call call = findCall(callId);

        call.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .ifPresent(p -> p.setStatus(ParticipantStatus.REJECTED));

        boolean anyJoined = call.getParticipants().stream()
                .anyMatch(p -> p.getStatus() == ParticipantStatus.JOINED);
        boolean anyRinging = call.getParticipants().stream()
                .anyMatch(p -> p.getStatus() == ParticipantStatus.RINGING);
        
        if (!anyJoined && !anyRinging) {
            finalizeCall(call, CallStatus.REJECTED);
        }

        notifyParticipantUpdate(call, userId, ParticipantStatus.REJECTED);
        return toFullResponse(call);
    }

    public CallResponse endCall(String callId, Long userId, EndCallRequest request) {
        Call call = findCall(callId);

        call.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .ifPresent(p -> {
                    p.setStatus(ParticipantStatus.LEFT);
                    p.setLeftAt(Instant.now());
                });

        long activeCount = call.getParticipants().stream()
                .filter(p -> p.getStatus() == ParticipantStatus.JOINED)
                .count();

        if (activeCount == 0) {
            finalizeCall(call, CallStatus.ENDED);
        }

        notifyParticipantUpdate(call, userId, ParticipantStatus.LEFT);
        return toFullResponse(call);
    }

    private void notifyParticipantUpdate(Call call, Long actorId, ParticipantStatus status) {
        User actor = userRepository.findById(actorId).orElse(null);
        CallParticipantUpdateDto update = CallParticipantUpdateDto.builder()
                .callId(call.getId())
                .userId(actorId)
                .fullName(actor != null ? actor.getFullName() : "Unknown")
                .avatarUrl(actor != null ? actor.getAvatarUrl() : null)
                .status(status)
                .build();

        // Notify caller
        if (!call.getCallerId().equals(actorId)) {
            webSocketService.sendCallParticipantUpdate(call.getCallerId(), update);
        }
        
        // Notify all participants
        for (CallParticipant p : call.getParticipants()) {
            if (!p.getUserId().equals(actorId)) {
                webSocketService.sendCallParticipantUpdate(p.getUserId(), update);
            }
        }
    }

    public void handleUserDisconnected(Long userId) {
        callRepository.findActiveCallByUserId(userId).ifPresent(call -> {
            log.warn("User {} disconnected from call {}", userId, call.getId());
            endCall(call.getId(), userId, new EndCallRequest());
        });
    }

    private void finalizeCall(Call call, CallStatus status) {
        timeoutScheduler.cancelTimeout(call.getId());
        call.end(status);
        callRepository.save(call);
    }

    private CallResponse toFullResponse(Call call) {
        CallResponse res = callMapper.toResponse(call);
        User caller = userRepository.findById(call.getCallerId()).orElse(null);
        if (caller != null) {
            res.setCallerName(caller.getFullName());
            res.setCallerAvatar(caller.getAvatarUrl());
        }
        
        if (res.getParticipants() != null) {
            for (var pRes : res.getParticipants()) {
                final var currentPRes = pRes;
                userRepository.findById(currentPRes.getUserId()).ifPresent(u -> {
                    currentPRes.setFullName(u.getFullName());
                    currentPRes.setAvatarUrl(u.getAvatarUrl());
                });
            }
        }
        return res;
    }

    private Call findCall(String id) {
        return callRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Call not found", "RESOURCE_NOT_FOUND"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));
    }

    private void validateUserBusy(Long userId) {
        if (callRepository.findActiveCallByUserId(userId).isPresent()) {
            throw BusinessException.badRequest("User is busy in another call", "USER_BUSY");
        }
    }

    private String generateAgoraToken(String channelId, String userId) {
        try {
            RtcTokenBuilder tokenBuilder = new RtcTokenBuilder();
            int timestamp = (int) (System.currentTimeMillis() / 1000 + agoraConfig.getDefaultTokenExpiration());
            return tokenBuilder.buildTokenWithUserAccount(
                    agoraConfig.getAppId(), agoraConfig.getAppCertificate(),
                    channelId, userId, Role.Role_Publisher, timestamp
            );
        } catch (Exception e) {
            throw new RuntimeException("Token generation failed", e);
        }
    }

    private void sendInvitation(Call call, User caller, Long calleeId) {
        webSocketService.sendCallInvitation(calleeId.toString(), CallInvitationDto.builder()
                .callId(call.getId())
                .channelId(call.getChannelId())
                .callerId(caller.getId())
                .callerName(caller.getFullName())
                .callerAvatar(caller.getAvatarUrl())
                .callType(call.getCallType())
                .build());
    }
}
