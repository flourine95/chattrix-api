package com.chattrix.api.services.call;

import com.chattrix.api.config.AgoraConfig;
import com.chattrix.api.entities.Call;
import com.chattrix.api.entities.CallStatus;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.exceptions.UnauthorizedException;
import com.chattrix.api.mappers.CallMapper;
import com.chattrix.api.repositories.CallRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.EndCallRequest;
import com.chattrix.api.requests.InitiateCallRequest;
import com.chattrix.api.requests.RejectCallRequest;
import com.chattrix.api.responses.CallConnectionResponse;
import com.chattrix.api.responses.CallResponse;
import com.chattrix.api.services.notification.WebSocketNotificationService;
import com.chattrix.api.websocket.dto.CallInvitationDto;
import io.agora.media.RtcTokenBuilder;
import io.agora.media.RtcTokenBuilder.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@ApplicationScoped
@Transactional
public class CallService {

    @Inject
    private CallRepository callRepository;
    @Inject
    private UserRepository userRepository;
    @Inject
    private CallMapper callMapper;
    @Inject
    private AgoraConfig agoraConfig;
    @Inject
    private WebSocketNotificationService webSocketService;
    @Inject
    private CallTimeoutScheduler timeoutScheduler;

    public CallConnectionResponse initiateCall(Long callerId, InitiateCallRequest request) {
        Long calleeId = request.getCalleeId();
        validateUserBusy(callerId, calleeId);

        String channelId = "channel_%d_%d_%d".formatted(System.currentTimeMillis(), callerId, calleeId);

        Call call = Call.builder()
                .id(UUID.randomUUID().toString())
                .channelId(channelId)
                .callerId(callerId)
                .calleeId(calleeId)
                .callType(request.getCallType())
                .status(CallStatus.RINGING)
                .build();

        call = callRepository.save(call);

        timeoutScheduler.scheduleTimeout(call.getId(), callerId.toString(), calleeId.toString());

        User caller = findUser(callerId);
        User callee = findUser(calleeId);

        sendInvitation(call, caller, callee);
        String token = generateAgoraToken(channelId, callerId.toString());

        return callMapper.toConnectionResponse(toFullResponse(call, caller, callee), token);
    }

    public CallConnectionResponse acceptCall(String callId, Long userId) {
        Call call = findCall(callId);

        if (!call.isCallee(userId)) {
            throw new UnauthorizedException("Only callee can accept this call");
        }

        try {
            call.accept();
        } catch (IllegalStateException e) {
            throw new BadRequestException("Call is not ringing", "INVALID_STATUS");
        }

        timeoutScheduler.cancelTimeout(callId);
        callRepository.save(call);

        webSocketService.sendCallAccepted(call.getCallerId().toString(), callId, userId.toString());
        String token = generateAgoraToken(call.getChannelId(), userId.toString());

        return callMapper.toConnectionResponse(toFullResponse(call), token);
    }

    public CallResponse rejectCall(String callId, Long userId, RejectCallRequest request) {
        Call call = findCall(callId);

        if (!call.isCallee(userId)) {
            throw new UnauthorizedException("Only callee can reject this call");
        }
        if (call.getStatus() != CallStatus.RINGING) {
            throw new BadRequestException("Cannot reject call", "INVALID_STATUS");
        }

        finalizeCall(call, CallStatus.REJECTED);

        webSocketService.sendCallRejected(
                call.getCallerId().toString(), callId, userId.toString(), request.getReason()
        );

        return toFullResponse(call);
    }

    public CallResponse endCall(String callId, Long userId, EndCallRequest request) {
        Call call = findCall(callId);

        if (!call.isParticipant(userId)) {
            throw new UnauthorizedException("You are not a participant of this call");
        }
        if (call.isFinished()) {
            throw new BadRequestException("Call already ended", "CALL_ALREADY_ENDED");
        }

        finalizeCall(call, CallStatus.ENDED);

        webSocketService.sendCallEnded(
                call.getOtherUserId(userId).toString(),
                callId,
                userId.toString(),
                call.getDurationSeconds()
        );

        return toFullResponse(call);
    }

    public void handleUserDisconnected(Long userId) {
        callRepository.findActiveCallByUserId(userId).ifPresent(call -> {
            log.warn("Force ending call {} due to user {} disconnection", call.getId(), userId);

            finalizeCall(call, CallStatus.ENDED);

            try {
                webSocketService.sendCallEnded(
                        call.getOtherUserId(userId).toString(),
                        call.getId(),
                        userId.toString(),
                        call.getDurationSeconds()
                );
            } catch (Exception e) {
                log.error("Failed to notify partner about forced disconnection", e);
            }
        });
    }

    private void finalizeCall(Call call, CallStatus status) {
        timeoutScheduler.cancelTimeout(call.getId());
        call.end(status);
        callRepository.save(call);
    }

    private CallResponse toFullResponse(Call call) {
        User caller = userRepository.findById(call.getCallerId()).orElse(null);
        User callee = userRepository.findById(call.getCalleeId()).orElse(null);
        return toFullResponse(call, caller, callee);
    }

    private CallResponse toFullResponse(Call call, User caller, User callee) {
        CallResponse res = callMapper.toResponse(call, caller, callee);
        if (caller != null) {
            res.setCallerName(caller.getFullName());
            res.setCallerAvatar(caller.getAvatarUrl());
        }
        if (callee != null) {
            res.setCalleeName(callee.getFullName());
            res.setCalleeAvatar(callee.getAvatarUrl());
        }
        return res;
    }

    private Call findCall(String id) {
        return callRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Call not found"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validateUserBusy(Long callerId, Long calleeId) {
        if (callRepository.findActiveCallByUserId(callerId).isPresent() ||
                callRepository.findActiveCallByUserId(calleeId).isPresent()) {
            throw new BadRequestException("User is busy in another call", "USER_BUSY");
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

    private void sendInvitation(Call call, User caller, User callee) {
        webSocketService.sendCallInvitation(callee.getId().toString(), CallInvitationDto.builder()
                .callId(call.getId())
                .channelId(call.getChannelId())
                .callerId(caller.getId())
                .callerName(caller.getFullName())
                .callerAvatar(caller.getAvatarUrl())
                .callType(call.getCallType())
                .build());
    }
}