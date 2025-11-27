package com.chattrix.api.services;

import com.chattrix.api.config.AgoraConfig;
import com.chattrix.api.entities.*;
import com.chattrix.api.exceptions.*;
import com.chattrix.api.mappers.CallMapper;
import com.chattrix.api.repositories.CallRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.*;
import com.chattrix.api.responses.*;
import com.chattrix.api.websocket.dto.CallInvitationDto;

import io.agora.media.RtcTokenBuilder;
import io.agora.media.RtcTokenBuilder.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@NoArgsConstructor(force = true)
@Slf4j
public class CallService {

    private final CallRepository callRepository;
    private final UserRepository userRepository;
    private final CallMapper callMapper;
    private final AgoraConfig agoraConfig;
    private final WebSocketNotificationService webSocketService;

    /**
     * 1. INITIATE: Tạo call -> Sinh Agora Token -> Trả về ConnectionResponse
     */
    public CallConnectionResponse initiateCall(Long callerId, InitiateCallRequest request) {
        Long calleeId = request.getCalleeId();
        log.info("Initiating call: {} -> {}", callerId, calleeId);

        // Validation: Kiểm tra xem 2 người này có đang bận không
        validateUserBusy(callerId, calleeId);

        // Tạo Channel ID duy nhất
        String channelId = "channel_%d_%d_%d".formatted(System.currentTimeMillis(), callerId, calleeId);

        // Lưu DB
        Call call = Call.builder()
                .id(UUID.randomUUID().toString())
                .channelId(channelId)
                .callerId(callerId)
                .calleeId(calleeId)
                .callType(request.getCallType())
                .status(CallStatus.RINGING)
                .build();

        call = callRepository.save(call);

        // --- LOGIC AGORA: Sinh Token cho Caller ---
        String token = generateAgoraToken(channelId, callerId.toString());

        // Gửi Socket mời Callee
        User caller = findUser(callerId);
        User callee = findUser(calleeId);
        sendInvitation(call, caller, callee);

        // Trả về Info + Token
        return callMapper.toConnectionResponse(buildResponse(call, caller, callee), token);
    }

    /**
     * 2. ACCEPT: Update status -> Sinh Agora Token -> Trả về ConnectionResponse
     */
    public CallConnectionResponse acceptCall(String callId, Long userId) {
        Call call = findCall(callId);

        // Chỉ người được gọi (Callee) mới được quyền bấm nghe
        if (!call.getCalleeId().equals(userId)) {
            throw new UnauthorizedException("Only callee can accept this call");
        }

        // Chỉ chấp nhận khi trạng thái đang đổ chuông
        if (call.getStatus() != CallStatus.RINGING) {
            throw new BadRequestException("Call is not ringing (Status: " + call.getStatus() + ")", "INVALID_STATUS");
        }

        // Update status
        call.setStatus(CallStatus.CONNECTING);
        call.setStartTime(Instant.now());
        callRepository.save(call);

        // --- LOGIC AGORA: Sinh Token cho Callee ---
        String token = generateAgoraToken(call.getChannelId(), userId.toString());

        // Báo cho Caller biết là bên kia đã nghe máy
        webSocketService.sendCallAccepted(
                call.getCallerId().toString(), callId, userId.toString()
        );

        // Trả về Info + Token
        return callMapper.toConnectionResponse(buildResponse(call), token);
    }

    /**
     * 3. REJECT: Update status -> KHÔNG Token
     */
    public CallResponse rejectCall(String callId, Long userId, RejectCallRequest request) {
        Call call = findCall(callId);

        // Chỉ người được gọi mới có quyền từ chối
        if (!call.getCalleeId().equals(userId)) {
            throw new UnauthorizedException("Only callee can reject this call");
        }

        // Nếu cuộc gọi đã kết thúc hoặc đã nghe rồi thì không từ chối được nữa
        if (call.getStatus() != CallStatus.RINGING && call.getStatus() != CallStatus.INITIATING) {
            throw new BadRequestException("Cannot reject call in status: " + call.getStatus(), "INVALID_STATUS");
        }

        call.setStatus(CallStatus.REJECTED);
        call.setEndTime(Instant.now());
        // Có thể lưu reason vào DB nếu entity Call có trường này
        // call.setEndReason(request.getReason());

        callRepository.save(call);

        webSocketService.sendCallRejected(
                call.getCallerId().toString(), callId, userId.toString(), request.getReason()
        );

        return buildResponse(call);
    }

    /**
     * 4. END: Update status -> KHÔNG Token
     */
    public CallResponse endCall(String callId, Long userId, EndCallRequest request) {
        Call call = findCall(callId);

        // Chỉ người trong cuộc (Caller hoặc Callee) mới được tắt máy
        if (!call.getCallerId().equals(userId) && !call.getCalleeId().equals(userId)) {
            throw new UnauthorizedException("You are not a participant of this call");
        }

        // Nếu đã tắt rồi thì thôi
        if (call.getStatus() == CallStatus.ENDED || call.getStatus() == CallStatus.REJECTED) {
            throw new BadRequestException("Call already ended", "CALL_ALREADY_ENDED");
        }

        call.setStatus(CallStatus.ENDED);
        call.setEndTime(Instant.now());

        // Tính duration (Server tự tính)
        int duration = 0;
        if (call.getStartTime() != null) {
            duration = (int) Duration.between(call.getStartTime(), call.getEndTime()).getSeconds();
        }
        call.setDurationSeconds(duration);

        callRepository.save(call);

        Long otherId = call.getCallerId().equals(userId) ? call.getCalleeId() : call.getCallerId();
        webSocketService.sendCallEnded(
                otherId.toString(), callId, userId.toString(), duration
        );

        return buildResponse(call);
    }

    // --- Private Helpers ---

    /**
     * Helper sinh Token Agora trực tiếp
     */
    private String generateAgoraToken(String channelId, String userId) {
        try {
            RtcTokenBuilder tokenBuilder = new RtcTokenBuilder();
            // Lấy thời gian hết hạn từ config (ví dụ 3600s)
            int expirationTimeInSeconds = agoraConfig.getDefaultTokenExpiration();
            int timestamp = (int) (System.currentTimeMillis() / 1000 + expirationTimeInSeconds);

            // Mặc định vai trò là Publisher
            return tokenBuilder.buildTokenWithUserAccount(
                    agoraConfig.getAppId(),
                    agoraConfig.getAppCertificate(),
                    channelId,
                    userId,
                    Role.Role_Publisher,
                    timestamp
            );
        } catch (Exception e) {
            log.error("Failed to generate Agora token for user {} in channel {}", userId, channelId, e);
            throw new RuntimeException("Token generation failed");
        }
    }

    private void sendInvitation(Call call, User caller, User callee) {
        // Dùng DTO mới với Builder pattern
        CallInvitationDto data = CallInvitationDto.builder()
                .callId(call.getId())
                .channelId(call.getChannelId())
                .callerId(caller.getId()) // Đã là Long
                .callerName(caller.getFullName())
                .callerAvatar(caller.getAvatarUrl())
                .callType(call.getCallType())
                .build();

        webSocketService.sendCallInvitation(callee.getId().toString(), data);
    }

    private CallResponse buildResponse(Call call) {
        User caller = userRepository.findById(call.getCallerId()).orElse(null);
        User callee = userRepository.findById(call.getCalleeId()).orElse(null);
        return buildResponse(call, caller, callee);
    }

    private CallResponse buildResponse(Call call, User caller, User callee) {
        CallResponse res = callMapper.toResponse(call);
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
        return callRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Logic kiểm tra xem Caller hoặc Callee có đang bận nghe cuộc gọi khác không
     */
    private void validateUserBusy(Long callerId, Long calleeId) {
        // Kiểm tra người gọi
        Optional<Call> activeCallCaller = callRepository.findActiveCallByUserId(callerId);
        if (activeCallCaller.isPresent()) {
            throw new BadRequestException("You are already in another call", "USER_BUSY");
        }

        // Kiểm tra người nghe
        Optional<Call> activeCallCallee = callRepository.findActiveCallByUserId(calleeId);
        if (activeCallCallee.isPresent()) {
            throw new BadRequestException("The user is currently busy in another call", "USER_BUSY");
        }
    }
}