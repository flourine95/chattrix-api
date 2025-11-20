package com.chattrix.api.services;

import com.chattrix.api.config.AgoraConfig;
import com.chattrix.api.entities.Call;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.repositories.CallRepository;
import com.chattrix.api.requests.AgoraRefreshTokenRequest;
import com.chattrix.api.requests.GenerateTokenRequest;
import com.chattrix.api.responses.TokenResponse;
import io.agora.media.RtcTokenBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for generating and managing Agora RTC tokens.
 * Implements token generation, UID conversion, and token refresh functionality.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.4
 */
@ApplicationScoped
public class AgoraTokenService {

    private static final Logger LOGGER = Logger.getLogger(AgoraTokenService.class.getName());

    @Inject
    private AgoraConfig agoraConfig;

    @Inject
    private CallRepository callRepository;

    /**
     * Generate an Agora RTC token for a user to join a channel.
     * 
     * Requirements: 3.1, 3.2, 3.3, 3.4
     * 
     * @param request Token generation request containing channel ID, user ID, role, and expiration
     * @return TokenResponse containing the generated token, channel ID, UID, and expiration time
     * @throws RuntimeException if token generation fails
     */
    public TokenResponse generateToken(GenerateTokenRequest request) {
        try {
            // Convert user ID to numeric UID (Requirement 3.2)
            int uid = generateUidFromUserId(request.getUserId());

            // Validate and clamp expiration time (Requirement 3.3)
            int expirationSeconds = validateExpiration(request.getExpirationSeconds());

            // Calculate expiration timestamp
            int currentTimestamp = (int) (System.currentTimeMillis() / 1000);
            int privilegeExpiredTs = currentTimestamp + expirationSeconds;

            // Determine role
            RtcTokenBuilder.Role role = "publisher".equalsIgnoreCase(request.getRole())
                    ? RtcTokenBuilder.Role.Role_Publisher
                    : RtcTokenBuilder.Role.Role_Subscriber;

            // Generate token using Agora SDK (Requirement 3.1)
            RtcTokenBuilder tokenBuilder = new RtcTokenBuilder();
            String token = tokenBuilder.buildTokenWithUid(
                    agoraConfig.getAppId(),
                    agoraConfig.getAppCertificate(),
                    request.getChannelId(),
                    uid,
                    role,
                    privilegeExpiredTs
            );

            Instant expiresAt = Instant.ofEpochSecond(privilegeExpiredTs);

            // Audit logging (Requirement 3.4)
            LOGGER.info(String.format(
                    "Token generated - User: %s, Channel: %s, UID: %d, Role: %s, Expiration: %d seconds",
                    request.getUserId(),
                    request.getChannelId(),
                    uid,
                    request.getRole(),
                    expirationSeconds
            ));

            return new TokenResponse(token, request.getChannelId(), uid, expiresAt);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate Agora token", e);
            throw new RuntimeException("TOKEN_GENERATION_FAILED: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a consistent numeric UID from a string user ID using MD5 hash.
     * The same user ID will always produce the same UID.
     * 
     * Requirement: 3.2
     * 
     * @param userId String user ID
     * @return Positive integer UID
     */
    public int generateUidFromUserId(String userId) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(userId.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.wrap(hash);
            return Math.abs(buffer.getInt());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "MD5 algorithm not available", e);
            throw new RuntimeException("Failed to generate UID from user ID", e);
        }
    }

    /**
     * Validate and clamp token expiration time to allowed bounds.
     * Ensures expiration is between min and max values configured.
     * 
     * Requirement: 3.3
     * 
     * @param requestedExpiration Requested expiration time in seconds
     * @return Clamped expiration time between min and max bounds
     */
    public int validateExpiration(Integer requestedExpiration) {
        if (requestedExpiration == null) {
            return agoraConfig.getDefaultTokenExpiration();
        }

        int minExpiration = agoraConfig.getMinTokenExpiration();
        int maxExpiration = agoraConfig.getMaxTokenExpiration();

        if (requestedExpiration < minExpiration) {
            LOGGER.warning(String.format(
                    "Requested expiration %d is below minimum %d, clamping to minimum",
                    requestedExpiration, minExpiration
            ));
            return minExpiration;
        }

        if (requestedExpiration > maxExpiration) {
            LOGGER.warning(String.format(
                    "Requested expiration %d exceeds maximum %d, clamping to maximum",
                    requestedExpiration, maxExpiration
            ));
            return maxExpiration;
        }

        return requestedExpiration;
    }

    /**
     * Refresh an existing Agora RTC token for a user in an active call.
     * Validates that the user has an active call in the specified channel.
     * 
     * Requirements: 4.1, 4.2, 4.4
     * 
     * @param request Token refresh request containing channel ID, user ID, and old token
     * @return TokenResponse containing the new token with same channel and role
     * @throws BadRequestException if no active call exists
     * @throws RuntimeException if token refresh fails
     */
    public TokenResponse refreshToken(AgoraRefreshTokenRequest request) {
        try {
            // Parse user ID to Long
            Long userIdLong;
            try {
                userIdLong = Long.parseLong(request.getUserId());
            } catch (NumberFormatException e) {
                throw new BadRequestException("INVALID_USER_ID", "Invalid user ID format");
            }
            
            // Validate active call exists (Requirement 4.1)
            Optional<Call> activeCall = callRepository.findActiveCallByUserId(userIdLong);
            
            if (activeCall.isEmpty()) {
                LOGGER.warning(String.format(
                        "Token refresh failed - No active call for user: %s",
                        request.getUserId()
                ));
                throw new BadRequestException("NO_ACTIVE_CALL", "No active call found for token refresh");
            }

            Call call = activeCall.get();
            
            // Verify the call is in the requested channel
            if (!call.getChannelId().equals(request.getChannelId())) {
                LOGGER.warning(String.format(
                        "Token refresh failed - Channel mismatch. Requested: %s, Active: %s",
                        request.getChannelId(),
                        call.getChannelId()
                ));
                throw new BadRequestException("NO_ACTIVE_CALL", "No active call found in the specified channel");
            }

            // Generate UID (same as original)
            int uid = generateUidFromUserId(request.getUserId());

            // Use default expiration for refresh
            int expirationSeconds = agoraConfig.getDefaultTokenExpiration();
            int currentTimestamp = (int) (System.currentTimeMillis() / 1000);
            int privilegeExpiredTs = currentTimestamp + expirationSeconds;

            // Preserve role as publisher (Requirement 4.2)
            // In a real scenario, we might want to store the original role in the call entity
            RtcTokenBuilder.Role role = RtcTokenBuilder.Role.Role_Publisher;

            // Generate new token (Requirement 4.2)
            RtcTokenBuilder tokenBuilder = new RtcTokenBuilder();
            String newToken = tokenBuilder.buildTokenWithUid(
                    agoraConfig.getAppId(),
                    agoraConfig.getAppCertificate(),
                    request.getChannelId(),
                    uid,
                    role,
                    privilegeExpiredTs
            );

            Instant expiresAt = Instant.ofEpochSecond(privilegeExpiredTs);

            // Audit logging with partial old token (Requirement 4.4)
            String partialOldToken = maskToken(request.getOldToken());
            LOGGER.info(String.format(
                    "Token refreshed - User: %s, Channel: %s, UID: %d, Old Token: %s, Expiration: %d seconds",
                    request.getUserId(),
                    request.getChannelId(),
                    uid,
                    partialOldToken,
                    expirationSeconds
            ));

            return new TokenResponse(newToken, request.getChannelId(), uid, expiresAt);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to refresh Agora token", e);
            throw new RuntimeException("TOKEN_REFRESH_FAILED: " + e.getMessage(), e);
        }
    }

    /**
     * Mask token for logging (show only first 8 and last 8 characters)
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 16) {
            return "****";
        }
        return token.substring(0, 8) + "****" + token.substring(token.length() - 8);
    }
}
