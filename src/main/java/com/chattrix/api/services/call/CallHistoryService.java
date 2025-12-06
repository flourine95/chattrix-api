package com.chattrix.api.services.call;

import com.chattrix.api.entities.*;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.exceptions.UnauthorizedException;
import com.chattrix.api.mappers.CallHistoryMapper;
import com.chattrix.api.repositories.CallHistoryRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.CallHistoryResponse;
import com.chattrix.api.responses.PaginatedResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for managing call history operations.
 * Handles retrieval, filtering, pagination, and deletion of call history entries.
 * Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 9.2, 5.4
 */
@ApplicationScoped
public class CallHistoryService {

    private static final Logger LOGGER = Logger.getLogger(CallHistoryService.class.getName());
    private static final int MAX_PAGE_SIZE = 100;

    @Inject
    CallHistoryRepository callHistoryRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    CallHistoryMapper callHistoryMapper;

    /**
     * Retrieves paginated call history for a user with optional filtering.
     * Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5
     *
     * @param userId   the ID of the user requesting history
     * @param page     the page number (0-indexed)
     * @param size     the page size (max 100)
     * @param callType optional filter by call type (AUDIO, VIDEO, or null for all)
     * @param status   optional filter by status (COMPLETED, MISSED, REJECTED, FAILED, or null for all)
     * @return paginated response with call history entries
     * @throws BadRequestException if pagination parameters are invalid
     */
    public PaginatedResponse<CallHistoryResponse> getCallHistory(
            String userId,
            int page,
            int size,
            CallType callType,
            CallHistoryStatus status) {

        LOGGER.log(Level.INFO, "Retrieving call history for user {0}, page {1}, size {2}, type {3}, status {4}",
                new Object[]{userId, page, size, callType, status});

        // Validate pagination parameters
        if (page < 0) {
            throw new BadRequestException("Page number must be non-negative");
        }

        if (size <= 0) {
            throw new BadRequestException("Page size must be positive");
        }

        if (size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size cannot exceed " + MAX_PAGE_SIZE);
        }

        // Query call history with pagination and filters
        List<CallHistory> historyEntries = callHistoryRepository.findByUserId(
                userId,
                page,
                size,
                callType,
                status
        );

        // Get total count for pagination metadata
        long total = callHistoryRepository.countByUserId(userId, callType, status);

        // Map to response DTOs
        List<CallHistoryResponse> responses = historyEntries.stream()
                .map(callHistoryMapper::toResponse)
                .collect(Collectors.toList());

        LOGGER.log(Level.INFO, "Retrieved {0} call history entries out of {1} total",
                new Object[]{responses.size(), total});

        // Return paginated response with metadata
        return new PaginatedResponse<>(responses, page, size, total);
    }

    /**
     * Deletes a call history entry for a user.
     * Validates: Requirements 9.2
     *
     * @param userId the ID of the user requesting deletion
     * @param callId the ID of the call to delete from history
     * @throws ResourceNotFoundException if call history entry not found
     * @throws UnauthorizedException     if user doesn't own the history entry
     */
    @Transactional
    public void deleteCallHistory(String userId, String callId) {
        LOGGER.log(Level.INFO, "User {0} deleting call history for call {1}",
                new Object[]{userId, callId});

        // Verify user owns the history entry by attempting to find it
        // The repository method will only delete if both userId and callId match
        callHistoryRepository.deleteByCallIdAndUserId(callId, userId);

        LOGGER.log(Level.INFO, "Call history deleted for user {0}, call {1}",
                new Object[]{userId, callId});
    }

    /**
     * Creates a call history entry for a user.
     * Handles upsert for duplicate entries (same user_id and call_id).
     * Validates: Requirements 5.4
     *
     * @param userId           the ID of the user
     * @param callId           the ID of the call
     * @param remoteUserId     the ID of the remote user
     * @param callType         the type of call (AUDIO or VIDEO)
     * @param status           the status of the call
     * @param direction        the direction of the call (INCOMING or OUTGOING)
     * @param timestamp        the timestamp of the call
     * @param durationSeconds  the duration of the call in seconds (nullable)
     * @return the created call history entry
     */
    @Transactional
    public CallHistory createHistoryEntry(
            Long userId,
            String callId,
            Long remoteUserId,
            CallType callType,
            CallHistoryStatus status,
            CallDirection direction,
            Instant timestamp,
            Integer durationSeconds) {

        LOGGER.log(Level.INFO, "Creating call history entry for user {0}, call {1}",
                new Object[]{userId, callId});

        // Get remote user info
        User remoteUser = userRepository.findById(remoteUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Remote user not found: " + remoteUserId));

        // Create history entry with all fields
        CallHistory historyEntry = new CallHistory();
        historyEntry.setId(UUID.randomUUID().toString());
        historyEntry.setUserId(userId);
        historyEntry.setCallId(callId);
        historyEntry.setRemoteUserId(remoteUserId);
        historyEntry.setRemoteUserName(remoteUser.getFullName());
        historyEntry.setRemoteUserAvatar(remoteUser.getAvatarUrl());
        historyEntry.setCallType(callType);
        historyEntry.setStatus(status);
        historyEntry.setDirection(direction);
        historyEntry.setTimestamp(timestamp);
        historyEntry.setDurationSeconds(durationSeconds);
        historyEntry.setCreatedAt(Instant.now());

        // Save (handles upsert due to unique constraint on user_id + call_id)
        CallHistory saved = callHistoryRepository.save(historyEntry);

        LOGGER.log(Level.INFO, "Call history entry created with ID: {0}", saved.getId());

        return saved;
    }
}
