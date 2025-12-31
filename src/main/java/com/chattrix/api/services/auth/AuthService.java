package com.chattrix.api.services.auth;

import com.chattrix.api.entities.InvalidatedToken;
import com.chattrix.api.entities.RefreshToken;
import com.chattrix.api.entities.User;
import com.chattrix.api.entities.UserToken;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.repositories.InvalidatedTokenRepository;
import com.chattrix.api.repositories.RefreshTokenRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.repositories.UserTokenRepository;
import com.chattrix.api.requests.ChangePasswordRequest;
import com.chattrix.api.requests.LoginRequest;
import com.chattrix.api.requests.RegisterRequest;
import com.chattrix.api.responses.AuthResponse;
import com.chattrix.api.responses.UserResponse;
import com.chattrix.api.services.cache.OnlineStatusCache;
import com.chattrix.api.services.common.AvatarService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserTokenRepository userTokenRepository;

    @Inject
    private TokenService tokenService;

    @Inject
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Inject
    private RefreshTokenRepository refreshTokenRepository;

    @Inject
    private VerificationService verificationService;

    @Inject
    private UserMapper userMapper;

    @Inject
    private AvatarService avatarService;

    @Inject
    private OnlineStatusCache onlineStatusCache;

    /**
     * Register new user
     * Flow: Request -> Mapper -> Entity -> Hash password -> Save -> Generate avatar -> Send verification email
     */
    @Transactional
    public void register(RegisterRequest request) {
        // 1. Map request to entity
        User newUser = userMapper.toEntity(request);
        
        // 2. Hash password
        String hashedPassword = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());
        newUser.setPassword(hashedPassword);
        
        // 3. Save user
        userRepository.save(newUser);

        // 4. Generate and upload avatar
        String avatarUrl = avatarService.generateAndUploadAvatar(
            newUser.getId().toString(), 
            newUser.getFullName()
        );
        newUser.setAvatarUrl(avatarUrl);
        userRepository.save(newUser);

        // 5. Send verification email
        verificationService.sendVerificationEmailByEmail(newUser.getEmail());
    }

    /**
     * Login user
     * Flow: Validate credentials -> Update online status -> Generate tokens -> Return response
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Find user by username or email
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail().trim())
                .orElseThrow(() -> BusinessException.unauthorized("Invalid username/email or password"));

        // 2. Validate password
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("Invalid username/email or password");
        }

        // 3. Check email verification
        if (!user.isEmailVerified()) {
            throw BusinessException.unauthorized(
                "Email not verified. Please check your email to verify your account."
            );
        }

        // 4. Update online status (cache only)
        onlineStatusCache.markOnline(user.getId());
        
        // 5. Update lastSeen in DB
        user.setLastSeen(Instant.now());
        userRepository.save(user);

        // 6. Generate tokens
        String jti = UUID.randomUUID().toString();
        String accessToken = tokenService.generateAccessToken(user, jti);
        RefreshToken refreshToken = tokenService.generateRefreshToken(user, jti, accessToken);

        // 7. Build and return response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(tokenService.getAccessTokenValidityInSeconds())
                .build();
    }

    /**
     * Get current user by ID
     * Flow: Find user -> Map to response
     */
    public UserResponse getCurrentUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));
        
        return userMapper.toResponse(user);
    }

    /**
     * Logout from current device
     * Flow: Update lastSeen -> Invalidate access token -> Revoke refresh token
     */
    @Transactional
    public void logout(Long userId, String accessToken) {
        // 1. Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        // 2. Mark offline in cache
        onlineStatusCache.markOffline(userId);
        
        // 3. Update lastSeen
        user.setLastSeen(Instant.now());
        userRepository.save(user);

        // 4. Invalidate access token
        Date expiration = tokenService.getExpirationFromToken(accessToken);
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .token(accessToken)
                .expiresAt(expiration.toInstant())
                .invalidatedAt(Instant.now())
                .build();
        invalidatedTokenRepository.save(invalidatedToken);

        // 5. Revoke refresh token
        String tokenId = tokenService.getTokenId(accessToken);
        refreshTokenRepository.findByAccessTokenId(tokenId).ifPresent(refreshToken -> {
            refreshToken.revoke();
            refreshTokenRepository.update(refreshToken);
        });
    }

    /**
     * Logout from all devices
     * Flow: Mark offline -> Update lastSeen -> Revoke all refresh tokens
     */
    @Transactional
    public void logoutAllDevices(Long userId) {
        // 1. Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        // 2. Mark offline in cache
        onlineStatusCache.markOffline(userId);
        
        // 3. Update lastSeen
        user.setLastSeen(Instant.now());
        userRepository.save(user);

        // 4. Revoke all refresh tokens
        refreshTokenRepository.revokeAllByUser(user);
    }

    /**
     * Refresh access token
     * Flow: Validate refresh token -> Invalidate old access token -> Generate new tokens
     */
    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        // 1. Find and validate refresh token
        RefreshToken refreshToken = refreshTokenRepository.findValidToken(refreshTokenString)
                .orElseThrow(() -> BusinessException.unauthorized("Invalid or expired refresh token"));

        User user = refreshToken.getUser();

        // 2. Invalidate old access token if still valid
        String oldAccessToken = refreshToken.getAccessToken();
        if (oldAccessToken != null && !oldAccessToken.isEmpty()) {
            try {
                Date expiration = tokenService.getExpirationFromToken(oldAccessToken);
                if (expiration.after(new Date())) {
                    InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                            .token(oldAccessToken)
                            .expiresAt(expiration.toInstant())
                            .invalidatedAt(Instant.now())
                            .build();
                    invalidatedTokenRepository.save(invalidatedToken);
                }
            } catch (Exception e) {
                // Token already expired or invalid, skip
            }
        }

        // 3. Revoke old refresh token
        refreshToken.revoke();
        refreshTokenRepository.update(refreshToken);

        // 4. Generate new tokens
        String jti = UUID.randomUUID().toString();
        String newAccessToken = tokenService.generateAccessToken(user, jti);
        RefreshToken newRefreshToken = tokenService.generateRefreshToken(user, jti, newAccessToken);

        // 5. Build and return response
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(tokenService.getAccessTokenValidityInSeconds())
                .build();
    }

    /**
     * Change password
     * Flow: Validate current password -> Validate new password -> Hash and save
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 1. Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        // 2. Validate current password
        if (!BCrypt.checkpw(request.getCurrentPassword(), user.getPassword())) {
            throw BusinessException.badRequest("Current password is incorrect", "BAD_REQUEST");
        }

        // 3. Validate new password is different
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw BusinessException.badRequest(
                "New password must be different from current password", 
                "BAD_REQUEST"
            );
        }

        // 4. Hash and save new password
        String hashedPassword = BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        userRepository.save(user);
    }
}






