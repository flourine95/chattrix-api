package com.chattrix.api.services;

import com.chattrix.api.entities.*;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.exceptions.UnauthorizedException;
import com.chattrix.api.mappers.CallHistoryMapper;
import com.chattrix.api.mappers.CallMapper;
import com.chattrix.api.repositories.CallHistoryRepository;
import com.chattrix.api.repositories.CallQualityMetricsRepository;
import com.chattrix.api.repositories.CallRepository;
import com.chattrix.api.repositories.ContactRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.EndCallRequest;
import com.chattrix.api.requests.InitiateCallRequest;
import com.chattrix.api.requests.RejectCallRequest;
import com.chattrix.api.requests.UpdateCallStatusRequest;
import com.chattrix.api.responses.CallResponse;
import com.chattrix.api.websocket.dto.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing call lifecycle operations.
 * Handles call initiation, acceptance, rejection, ending, and status updates.
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 2.2, 2.3, 5.1-5.5, 8.1-8.4, 9.2
 */
@ApplicationScoped
public class CallService {

    private static final Logger LOGGER = Logger.getLogger(CallService.class.getName());

    @Inject
    CallRepository callRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ContactRepository contactRepository;

    @Inject
    CallHistoryRepository callHistoryRepository;

    @Inject
    CallQualityMetricsRepository callQualityMetricsRepository;

    @Inject
    WebSocketNotificationService webSocketNotificationService;

    @Inject
    CallTimeoutScheduler callTimeoutScheduler;

    @Inject
    CallMapper callMapper;

    @Inject
    CallHistoryMapper callHistoryMapper;

    // State machine for valid call status transitions
    private static final Map<CallStatus, Set<CallStatus>> VALID_TRANSITIONS = Map.of(
            CallStatus.INITIATING, Set.of(CallStatus.RINGING, CallStatus.REJECTED, CallStatus.MISSED, CallStatus.FAILED),
            CallStatus.RINGING, Set.of(CallStatus.CONNECTING, CallStatus.REJECTED, CallStatus.MISSED, CallStatus.FAILED),
            CallStatus.CONNECTING, Set.of(CallStatus.CONNECTED, CallStatus.DISCONNECTING, CallStatus.FAILED),
            CallStatus.CONNECTED, Set.of(CallStatus.DISCONNECTING),
            CallStatus.DISCONNECTING, Set.of(CallStatus.ENDED)
    );

    /**
     * Initiates a new call between caller and callee.
     * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 3.1, 3.2
     *
     * @param callerId the ID of the user initiating the call
     * @param request  the call initiation request
     * @return the created call response
     * @throws ResourceNotFoundException if callee doesn't exist
     * @throws UnauthorizedException     if users are not contacts
     * @throws BadRequestException       if either user is already in a call
     */
    @Transactional
    public CallResponse initiateCall(String callerId, InitiateCallRequest request) {
        LOGGER.log(Level.INFO, "Initiating call from {0} to {1}", new Object[]{callerId, request.getCalleeId()});

        // Validate callee exists
        Long calleeIdLong = parseUserId(request.getCalleeId());
        User callee = userRepository.findById(calleeIdLong)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getCalleeId()));

        // Validate caller exists
        Long callerIdLong = parseUserId(callerId);
        User caller = userRepository.findById(callerIdLong)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + callerId));

        // Check if users are contacts
        if (!areContacts(callerIdLong, calleeIdLong)) {
            throw new UnauthorizedException("Users are not contacts");
        }

        // Check if caller is already in a call
        Optional<Call> callerActiveCall = callRepository.findActiveCallByUserId(callerIdLong);
        if (callerActiveCall.isPresent()) {
            Call activeCall = callerActiveCall.get();
            LOGGER.log(Level.WARNING, "Caller {0} already in call: ID={1}, Status={2}, Created={3}", 
                new Object[]{callerId, activeCall.getId(), activeCall.getStatus(), activeCall.getCreatedAt()});
            throw new BadRequestException("User is already in a call", "USER_BUSY");
        }

        // Check if callee is already in a call
        Optional<Call> calleeActiveCall = callRepository.findActiveCallByUserId(calleeIdLong);
        if (calleeActiveCall.isPresent()) {
            Call activeCall = calleeActiveCall.get();
            LOGGER.log(Level.WARNING, "Callee {0} already in call: ID={1}, Status={2}, Created={3}", 
                new Object[]{request.getCalleeId(), activeCall.getId(), activeCall.getStatus(), activeCall.getCreatedAt()});
            throw new BadRequestException("Callee is already in a call", "USER_BUSY");
        }

        // Generate unique channel ID using format: channel_{timestamp}_{callerId}_{calleeId}
        String channelId = generateChannelId(callerId, request.getCalleeId());

        // Create call record with INITIATING status
        Call call = new Call();
        call.setId(UUID.randomUUID().toString()); // Generate unique call ID
        call.setChannelId(channelId); // Use backend-generated channel ID
        call.setCallerId(callerIdLong);
        call.setCalleeId(calleeIdLong);
        call.setCallType(request.getCallType());
        call.setStatus(CallStatus.INITIATING);
        call.setCreatedAt(Instant.now());
        call.setUpdatedAt(Instant.now());

        call = callRepository.save(call);
        LOGGER.log(Level.INFO, "Call created with ID: {0}, channel ID: {1}, status: INITIATING", 
                new Object[]{call.getId(), channelId});

        // Send WebSocket invitation to callee
        CallInvitationData invitationData = new CallInvitationData();
        invitationData.setCallId(call.getId());
        invitationData.setChannelId(call.getChannelId());
        invitationData.setCallerId(callerId);
        invitationData.setCallerName(caller.getFullName());
        invitationData.setCallerAvatar(caller.getAvatarUrl());
        invitationData.setCallType(request.getCallType());

        webSocketNotificationService.sendCallInvitation(request.getCalleeId(), invitationData);
        LOGGER.log(Level.INFO, "Call invitation sent to callee: {0}", request.getCalleeId());

        // Update status to RINGING
        call.setStatus(CallStatus.RINGING);
        call.setUpdatedAt(Instant.now());
        call = callRepository.save(call);
        LOGGER.log(Level.INFO, "Call status updated to RINGING");

        // Schedule 60-second timeout
        callTimeoutScheduler.scheduleTimeout(call.getId(), callerId, request.getCalleeId());
        LOGGER.log(Level.INFO, "Call timeout scheduled for 60 seconds");

        // Build and return response
        return buildCallResponse(call, caller, callee);
    }

    /**
     * Accepts an incoming call.
     * Validates: Requirements 2.2, 2.4
     *
     * @param callId the ID of the call to accept
     * @param userId the ID of the user accepting the call
     * @return the updated call response
     * @throws ResourceNotFoundException if call not found
     * @throws UnauthorizedException     if user is not the callee
     * @throws BadRequestException       if call status is not RINGING or call has timed out
     */
    @Transactional
    public CallResponse acceptCall(String callId, String userId) {
        LOGGER.log(Level.INFO, "User {0} accepting call {1}", new Object[]{userId, callId});

        // Find the call
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found: " + callId));

        Long userIdLong = parseUserId(userId);

        // Verify user is callee
        if (!call.getCalleeId().equals(userIdLong)) {
            throw new UnauthorizedException("User is not the callee of this call");
        }

        // Check call status is RINGING
        if (call.getStatus() != CallStatus.RINGING) {
            throw new BadRequestException("Call cannot be accepted in current status: " + call.getStatus(), "INVALID_CALL_STATUS");
        }

        // Check call has not timed out (created more than 60 seconds ago)
        Instant now = Instant.now();
        Duration timeSinceCreation = Duration.between(call.getCreatedAt(), now);
        if (timeSinceCreation.getSeconds() > 60) {
            throw new BadRequestException("Call has timed out", "CALL_TIMEOUT");
        }

        // Update status to CONNECTING
        call.setStatus(CallStatus.CONNECTING);
        call.setUpdatedAt(now);
        call = callRepository.save(call);
        LOGGER.log(Level.INFO, "Call status updated to CONNECTING");

        // Send WebSocket notification to caller
        webSocketNotificationService.sendCallAccepted(
                String.valueOf(call.getCallerId()), 
                callId, 
                userId
        );
        LOGGER.log(Level.INFO, "Call accepted notification sent to caller: {0}", call.getCallerId());

        // Get user details for response
        User caller = userRepository.findById(call.getCallerId()).orElse(null);
        User callee = userRepository.findById(call.getCalleeId()).orElse(null);

        return buildCallResponse(call, caller, callee);
    }

    /**
     * Rejects an incoming call.
     * Validates: Requirements 2.3
     *
     * @param callId  the ID of the call to reject
     * @param request the rejection request containing user ID and reason
     * @return the updated call response
     * @throws ResourceNotFoundException if call not found
     * @throws UnauthorizedException     if user is not the callee
     * @throws BadRequestException       if call status is not RINGING or INITIATING
     */
    @Transactional
    public CallResponse rejectCall(String callId, RejectCallRequest request) {
        LOGGER.log(Level.INFO, "User {0} rejecting call {1} with reason: {2}", 
                new Object[]{request.getUserId(), callId, request.getReason()});

        // Find the call
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found: " + callId));

        Long userIdLong = parseUserId(request.getUserId());

        // Verify user is callee
        if (!call.getCalleeId().equals(userIdLong)) {
            throw new UnauthorizedException("User is not the callee of this call");
        }

        // Check call status is RINGING or INITIATING
        if (call.getStatus() != CallStatus.RINGING && call.getStatus() != CallStatus.INITIATING) {
            throw new BadRequestException("Call cannot be rejected in current status: " + call.getStatus(), "INVALID_CALL_STATUS");
        }

        // Update status to REJECTED and store rejection reason
        call.setStatus(CallStatus.REJECTED);
        call.setEndTime(Instant.now());
        call.setUpdatedAt(Instant.now());
        // Note: rejection reason would be stored in a separate field if added to entity
        call = callRepository.save(call);
        LOGGER.log(Level.INFO, "Call status updated to REJECTED");

        // Send WebSocket notification to caller
        webSocketNotificationService.sendCallRejected(
                String.valueOf(call.getCallerId()), 
                callId, 
                request.getUserId(), 
                request.getReason()
        );
        LOGGER.log(Level.INFO, "Call rejected notification sent to caller: {0}", call.getCallerId());

        // Get user details for response
        User caller = userRepository.findById(call.getCallerId()).orElse(null);
        User callee = userRepository.findById(call.getCalleeId()).orElse(null);

        return buildCallResponse(call, caller, callee);
    }

    /**
     * Ends an active call.
     * Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5
     *
     * @param callId  the ID of the call to end
     * @param request the end call request
     * @return the updated call response
     * @throws ResourceNotFoundException if call not found
     * @throws UnauthorizedException     if user is not a participant
     * @throws BadRequestException       if call has already ended
     */
    @Transactional
    public CallResponse endCall(String callId, EndCallRequest request) {
        LOGGER.log(Level.INFO, "User {0} ending call {1}", new Object[]{request.getUserId(), callId});

        // Find the call
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found: " + callId));

        Long userIdLong = parseUserId(request.getUserId());

        // Verify user is a participant
        if (!call.getCallerId().equals(userIdLong) && !call.getCalleeId().equals(userIdLong)) {
            throw new UnauthorizedException("User is not a participant of this call");
        }

        // Check call has not already ended
        if (call.getStatus() == CallStatus.ENDED || call.getStatus() == CallStatus.REJECTED || 
            call.getStatus() == CallStatus.MISSED || call.getStatus() == CallStatus.FAILED) {
            throw new BadRequestException("Call has already ended", "CALL_ALREADY_ENDED");
        }

        // Calculate duration if not provided
        Instant endTime = Instant.now();
        Integer durationSeconds = request.getDurationSeconds();
        if (durationSeconds == null && call.getStartTime() != null) {
            durationSeconds = (int) Duration.between(call.getStartTime(), endTime).getSeconds();
        }

        // Update status to ENDED
        call.setStatus(CallStatus.ENDED);
        call.setEndTime(endTime);
        call.setDurationSeconds(durationSeconds);
        call.setUpdatedAt(endTime);
        call = callRepository.save(call);
        LOGGER.log(Level.INFO, "Call status updated to ENDED with duration: {0} seconds", durationSeconds);

        // Send WebSocket notification to other participant
        Long otherParticipantIdLong = call.getCallerId().equals(userIdLong) ? 
                call.getCalleeId() : call.getCallerId();
        String otherParticipantId = String.valueOf(otherParticipantIdLong);

        webSocketNotificationService.sendCallEnded(
                otherParticipantId, 
                callId, 
                request.getUserId(), 
                durationSeconds
        );
        LOGGER.log(Level.INFO, "Call ended notification sent to: {0}", otherParticipantId);

        // Create call history entries for both participants
        createCallHistoryEntries(call);

        // Calculate average quality metrics if available
        calculateAverageQualityMetrics(callId);

        // Get user details for response
        User caller = userRepository.findById(call.getCallerId()).orElse(null);
        User callee = userRepository.findById(call.getCalleeId()).orElse(null);

        return buildCallResponse(call, caller, callee);
    }

    /**
     * Updates the status of a call.
     * Validates: Requirements 8.1, 8.2, 8.3, 8.4
     *
     * @param callId  the ID of the call to update
     * @param request the status update request
     * @return the updated call response
     * @throws ResourceNotFoundException if call not found
     * @throws UnauthorizedException     if user is not a participant
     * @throws BadRequestException       if status transition is invalid
     */
    @Transactional
    public CallResponse updateCallStatus(String callId, UpdateCallStatusRequest request) {
        LOGGER.log(Level.INFO, "User {0} updating call {1} status to {2}", 
                new Object[]{request.getUserId(), callId, request.getStatus()});

        // Find the call
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found: " + callId));

        Long userIdLong = parseUserId(request.getUserId());

        // Verify user is a participant
        if (!call.getCallerId().equals(userIdLong) && !call.getCalleeId().equals(userIdLong)) {
            throw new UnauthorizedException("User is not a participant of this call");
        }

        // Validate status transition using state machine
        CallStatus currentStatus = call.getStatus();
        CallStatus newStatus = request.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new BadRequestException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus),
                    "INVALID_STATUS_TRANSITION"
            );
        }

        // Update status in database
        call.setStatus(newStatus);
        call.setUpdatedAt(Instant.now());

        // Record start_time when status becomes CONNECTED
        if (newStatus == CallStatus.CONNECTED && call.getStartTime() == null) {
            call.setStartTime(Instant.now());
            LOGGER.log(Level.INFO, "Call start time recorded");
        }

        call = callRepository.save(call);
        LOGGER.log(Level.INFO, "Call status updated to {0}", newStatus);

        // Notify other participant
        Long otherParticipantIdLong = call.getCallerId().equals(userIdLong) ? 
                call.getCalleeId() : call.getCallerId();
        String otherParticipantId = String.valueOf(otherParticipantIdLong);

        // Send appropriate notification based on new status
        notifyStatusChange(otherParticipantId, call);

        // Get user details for response
        User caller = userRepository.findById(call.getCallerId()).orElse(null);
        User callee = userRepository.findById(call.getCalleeId()).orElse(null);

        return buildCallResponse(call, caller, callee);
    }

    /**
     * Gets details of a specific call.
     * Validates: Requirements 9.2
     *
     * @param callId the ID of the call
     * @param userId the ID of the user requesting details
     * @return the call details
     * @throws ResourceNotFoundException if call not found
     * @throws UnauthorizedException     if user is not a participant
     */
    public CallResponse getCallDetails(String callId, String userId) {
        LOGGER.log(Level.INFO, "User {0} requesting details for call {1}", new Object[]{userId, callId});

        // Find the call
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found: " + callId));

        Long userIdLong = parseUserId(userId);

        // Verify user is a participant
        if (!call.getCallerId().equals(userIdLong) && !call.getCalleeId().equals(userIdLong)) {
            throw new UnauthorizedException("User is not a participant of this call");
        }

        // Get user details for response
        User caller = userRepository.findById(call.getCallerId()).orElse(null);
        User callee = userRepository.findById(call.getCalleeId()).orElse(null);

        return buildCallResponse(call, caller, callee);
    }

    /**
     * Accepts an incoming call via WebSocket.
     * Validates: Requirements 2.3, 2.4, 2.5
     *
     * @param callId the ID of the call to accept
     * @param userId the ID of the user accepting the call
     * @return the updated call response
     * @throws ResourceNotFoundException if call not found
     * @throws UnauthorizedException     if user is not the callee
     * @throws BadRequestException       if call status is not RINGING or call has timed out
     */
    @Transactional
    public CallResponse acceptCallViaWebSocket(String callId, String userId) {
        LOGGER.log(Level.INFO, "User {0} accepting call {1} via WebSocket", new Object[]{userId, callId});

        // Find the call
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found: " + callId));

        Long userIdLong = parseUserId(userId);

        // Verify user is callee
        if (!call.getCalleeId().equals(userIdLong)) {
            throw new UnauthorizedException("User is not the callee of this call");
        }

        // Check call status is RINGING
        if (call.getStatus() != CallStatus.RINGING) {
            throw new BadRequestException("Call cannot be accepted in current status: " + call.getStatus(), "INVALID_CALL_STATUS");
        }

        // Check call has not timed out (created more than 60 seconds ago)
        Instant now = Instant.now();
        Duration timeSinceCreation = Duration.between(call.getCreatedAt(), now);
        if (timeSinceCreation.getSeconds() > 60) {
            throw new BadRequestException("Call has timed out", "CALL_TIMEOUT");
        }

        // Update status to CONNECTING
        call.setStatus(CallStatus.CONNECTING);
        call.setUpdatedAt(now);
        call = callRepository.save(call);
        LOGGER.log(Level.INFO, "Call status updated to CONNECTING");

        // Send WebSocket notification to caller
        webSocketNotificationService.sendCallAccepted(
                String.valueOf(call.getCallerId()), 
                callId, 
                userId
        );
        LOGGER.log(Level.INFO, "Call accepted notification sent to caller: {0}", call.getCallerId());

        // Get user details for response
        User caller = userRepository.findById(call.getCallerId()).orElse(null);
        User callee = userRepository.findById(call.getCalleeId()).orElse(null);

        return buildCallResponse(call, caller, callee);
    }

    /**
     * Rejects an incoming call via WebSocket.
     * Validates: Requirements 3.3, 3.4, 3.5
     *
     * @param callId the ID of the call to reject
     * @param userId the ID of the user rejecting the call
     * @param reason the reason for rejection
     * @return the updated call response
     * @throws ResourceNotFoundException if call not found
     * @throws UnauthorizedException     if user is not the callee
     * @throws BadRequestException       if call status is not RINGING or INITIATING
     */
    @Transactional
    public CallResponse rejectCallViaWebSocket(String callId, String userId, String reason) {
        LOGGER.log(Level.INFO, "User {0} rejecting call {1} via WebSocket with reason: {2}", 
                new Object[]{userId, callId, reason});

        // Find the call
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found: " + callId));

        Long userIdLong = parseUserId(userId);

        // Verify user is callee
        if (!call.getCalleeId().equals(userIdLong)) {
            throw new UnauthorizedException("User is not the callee of this call");
        }

        // Check call status is RINGING or INITIATING
        if (call.getStatus() != CallStatus.RINGING && call.getStatus() != CallStatus.INITIATING) {
            throw new BadRequestException("Call cannot be rejected in current status: " + call.getStatus(), "INVALID_CALL_STATUS");
        }

        // Update status to REJECTED and store rejection reason
        call.setStatus(CallStatus.REJECTED);
        call.setEndTime(Instant.now());
        call.setUpdatedAt(Instant.now());
        call = callRepository.save(call);
        LOGGER.log(Level.INFO, "Call status updated to REJECTED");

        // Send WebSocket notification to caller
        webSocketNotificationService.sendCallRejected(
                String.valueOf(call.getCallerId()), 
                callId, 
                userId, 
                reason
        );
        LOGGER.log(Level.INFO, "Call rejected notification sent to caller: {0}", call.getCallerId());

        // Get user details for response
        User caller = userRepository.findById(call.getCallerId()).orElse(null);
        User callee = userRepository.findById(call.getCalleeId()).orElse(null);

        return buildCallResponse(call, caller, callee);
    }

    /**
     * Ends an active call via WebSocket.
     * Validates: Requirements 4.3, 4.4, 4.5, 4.6
     *
     * @param callId          the ID of the call to end
     * @param userId          the ID of the user ending the call
     * @param durationSeconds the duration in seconds (optional)
     * @return the updated call response
     * @throws ResourceNotFoundException if call not found
     * @throws UnauthorizedException     if user is not a participant
     * @throws BadRequestException       if call has already ended
     */
    @Transactional
    public CallResponse endCallViaWebSocket(String callId, String userId, Integer durationSeconds) {
        LOGGER.log(Level.INFO, "User {0} ending call {1} via WebSocket", new Object[]{userId, callId});

        // Find the call
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found: " + callId));

        Long userIdLong = parseUserId(userId);

        // Verify user is a participant
        if (!call.getCallerId().equals(userIdLong) && !call.getCalleeId().equals(userIdLong)) {
            throw new UnauthorizedException("User is not a participant of this call");
        }

        // Check call has not already ended
        if (call.getStatus() == CallStatus.ENDED || call.getStatus() == CallStatus.REJECTED || 
            call.getStatus() == CallStatus.MISSED || call.getStatus() == CallStatus.FAILED) {
            throw new BadRequestException("Call has already ended", "CALL_ALREADY_ENDED");
        }

        // Calculate duration if not provided
        Instant endTime = Instant.now();
        if (durationSeconds == null && call.getStartTime() != null) {
            durationSeconds = (int) Duration.between(call.getStartTime(), endTime).getSeconds();
        }

        // Update status to ENDED
        call.setStatus(CallStatus.ENDED);
        call.setEndTime(endTime);
        call.setDurationSeconds(durationSeconds);
        call.setUpdatedAt(endTime);
        call = callRepository.save(call);
        LOGGER.log(Level.INFO, "Call status updated to ENDED with duration: {0} seconds", durationSeconds);

        // Send WebSocket notification to other participant
        Long otherParticipantIdLong = call.getCallerId().equals(userIdLong) ? 
                call.getCalleeId() : call.getCallerId();
        String otherParticipantId = String.valueOf(otherParticipantIdLong);

        webSocketNotificationService.sendCallEnded(
                otherParticipantId, 
                callId, 
                userId, 
                durationSeconds
        );
        LOGGER.log(Level.INFO, "Call ended notification sent to: {0}", otherParticipantId);

        // Create call history entries for both participants
        createCallHistoryEntries(call);

        // Calculate average quality metrics if available
        calculateAverageQualityMetrics(callId);

        // Get user details for response
        User caller = userRepository.findById(call.getCallerId()).orElse(null);
        User callee = userRepository.findById(call.getCalleeId()).orElse(null);

        return buildCallResponse(call, caller, callee);
    }

    // Helper methods

    private boolean areContacts(Long userId1, Long userId2) {
        return contactRepository.findByUserIdAndContactUserId(userId1, userId2)
                .map(contact -> contact.getStatus() == Contact.ContactStatus.ACCEPTED)
                .orElse(false);
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid user ID format: " + userId);
        }
    }

    private boolean isValidTransition(CallStatus from, CallStatus to) {
        Set<CallStatus> allowedTransitions = VALID_TRANSITIONS.get(from);
        return allowedTransitions != null && allowedTransitions.contains(to);
    }

    private CallResponse buildCallResponse(Call call, User caller, User callee) {
        CallResponse response = callMapper.toResponse(call);
        if (caller != null) {
            response.setCallerName(caller.getFullName());
            response.setCallerAvatar(caller.getAvatarUrl());
        }
        if (callee != null) {
            response.setCalleeName(callee.getFullName());
            response.setCalleeAvatar(callee.getAvatarUrl());
        }
        return response;
    }

    private void createCallHistoryEntries(Call call) {
        LOGGER.log(Level.INFO, "Creating call history entries for call {0}", call.getId());

        // Get user details
        User caller = userRepository.findById(call.getCallerId()).orElse(null);
        User callee = userRepository.findById(call.getCalleeId()).orElse(null);

        if (caller == null || callee == null) {
            LOGGER.log(Level.WARNING, "Cannot create call history: user not found");
            return;
        }

        // Determine history status based on call status
        CallHistoryStatus historyStatus = mapCallStatusToHistoryStatus(call.getStatus());

        // Create history entry for caller (OUTGOING)
        CallHistory callerHistory = new CallHistory();
        callerHistory.setId(UUID.randomUUID().toString());
        callerHistory.setUserId(call.getCallerId());
        callerHistory.setCallId(call.getId());
        callerHistory.setRemoteUserId(call.getCalleeId());
        callerHistory.setRemoteUserName(callee.getFullName());
        callerHistory.setRemoteUserAvatar(callee.getAvatarUrl());
        callerHistory.setCallType(call.getCallType());
        callerHistory.setStatus(historyStatus);
        callerHistory.setDirection(CallDirection.OUTGOING);
        callerHistory.setTimestamp(call.getCreatedAt());
        callerHistory.setDurationSeconds(call.getDurationSeconds());
        callerHistory.setCreatedAt(Instant.now());

        callHistoryRepository.save(callerHistory);
        LOGGER.log(Level.INFO, "Call history entry created for caller");

        // Create history entry for callee (INCOMING)
        CallHistory calleeHistory = new CallHistory();
        calleeHistory.setId(UUID.randomUUID().toString());
        calleeHistory.setUserId(call.getCalleeId());
        calleeHistory.setCallId(call.getId());
        calleeHistory.setRemoteUserId(call.getCallerId());
        calleeHistory.setRemoteUserName(caller.getFullName());
        calleeHistory.setRemoteUserAvatar(caller.getAvatarUrl());
        calleeHistory.setCallType(call.getCallType());
        calleeHistory.setStatus(historyStatus);
        calleeHistory.setDirection(CallDirection.INCOMING);
        calleeHistory.setTimestamp(call.getCreatedAt());
        calleeHistory.setDurationSeconds(call.getDurationSeconds());
        calleeHistory.setCreatedAt(Instant.now());

        callHistoryRepository.save(calleeHistory);
        LOGGER.log(Level.INFO, "Call history entry created for callee");
    }

    private CallHistoryStatus mapCallStatusToHistoryStatus(CallStatus callStatus) {
        return switch (callStatus) {
            case ENDED -> CallHistoryStatus.COMPLETED;
            case MISSED -> CallHistoryStatus.MISSED;
            case REJECTED -> CallHistoryStatus.REJECTED;
            default -> CallHistoryStatus.FAILED;
        };
    }

    private void calculateAverageQualityMetrics(String callId) {
        LOGGER.log(Level.INFO, "Calculating average quality metrics for call {0}", callId);

        List<CallQualityMetrics> metrics = callQualityMetricsRepository.findByCallId(callId);
        if (metrics.isEmpty()) {
            LOGGER.log(Level.INFO, "No quality metrics found for call {0}", callId);
            return;
        }

        // Calculate averages
        double avgPacketLoss = metrics.stream()
                .filter(m -> m.getPacketLossRate() != null)
                .mapToDouble(CallQualityMetrics::getPacketLossRate)
                .average()
                .orElse(0.0);

        double avgRtt = metrics.stream()
                .filter(m -> m.getRoundTripTime() != null)
                .mapToInt(CallQualityMetrics::getRoundTripTime)
                .average()
                .orElse(0.0);

        LOGGER.log(Level.INFO, "Average quality metrics - Packet Loss: {0}, RTT: {1}", 
                new Object[]{avgPacketLoss, avgRtt});
        // Note: These averages could be stored in a separate table or returned in statistics
    }

    private void notifyStatusChange(String userId, Call call) {
        // This is a simplified notification - in a real implementation,
        // you might send different message types based on the status
        LOGGER.log(Level.INFO, "Notifying user {0} of status change to {1}", 
                new Object[]{userId, call.getStatus()});
        // Additional WebSocket notifications could be sent here based on status
    }

    /**
     * Generates a unique channel ID for Agora RTC.
     * Format: channel_{timestamp}_{callerId}_{calleeId}
     * Validates: Requirements 3.1
     *
     * @param callerId the ID of the caller
     * @param calleeId the ID of the callee
     * @return the generated channel ID
     */
    private String generateChannelId(String callerId, String calleeId) {
        long timestamp = System.currentTimeMillis();
        return String.format("channel_%d_%s_%s", timestamp, callerId, calleeId);
    }
}
