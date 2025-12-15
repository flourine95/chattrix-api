package com.chattrix.api.services.auth;

import com.chattrix.api.entities.InvalidatedToken;
import com.chattrix.api.entities.RefreshToken;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.repositories.InvalidatedTokenRepository;
import com.chattrix.api.repositories.RefreshTokenRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.ChangePasswordRequest;
import com.chattrix.api.requests.LoginRequest;
import com.chattrix.api.requests.RegisterRequest;
import com.chattrix.api.requests.ResendVerificationRequest;
import com.chattrix.api.responses.AuthResponse;
import com.chattrix.api.responses.UserResponse;
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

    @Transactional
    public void register(RegisterRequest registerRequest) {
        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername().trim());
        newUser.setEmail(registerRequest.getEmail().trim());
        newUser.setFullName(registerRequest.getFullName().trim());
        String hashedPassword = BCrypt.hashpw(registerRequest.getPassword(), BCrypt.gensalt());
        newUser.setPassword(hashedPassword);

        // Lưu user trước để có ID
        userRepository.save(newUser);

        // Tạo avatar tự động từ Cloudinary
        try {
            String avatarUrl = avatarService.generateAndUploadAvatar(
                    String.valueOf(newUser.getId()),
                    newUser.getFullName()
            );
            newUser.setAvatarUrl(avatarUrl);
            userRepository.save(newUser);
        } catch (Exception e) {
            // Log error nhưng không fail registration nếu avatar tạo lỗi
            // Avatar có thể được tạo lại sau
            System.err.println("Failed to generate avatar for user " + newUser.getId() + ": " + e.getMessage());
        }

        ResendVerificationRequest resendVerificationRequest = new ResendVerificationRequest();
        resendVerificationRequest.setEmail(registerRequest.getEmail());
        verificationService.sendVerificationEmail(resendVerificationRequest);
    }

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail().trim())
                .orElseThrow(() -> BusinessException.unauthorized("Invalid username/email or password"));

        if (!BCrypt.checkpw(loginRequest.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("Invalid username/email or password");
        }

        if (!user.isEmailVerified()) {
            throw BusinessException.unauthorized("Email not verified. Please check your email to verify your account.");
        }

        user.setOnline(true);
        user.setLastSeen(Instant.now());
        userRepository.save(user);

        String jti = UUID.randomUUID().toString();
        String accessToken = tokenService.generateAccessToken(user, jti);
        RefreshToken refreshToken = tokenService.generateRefreshToken(user, jti, accessToken);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                tokenService.getAccessTokenValidityInSeconds()
        );
    }


    public UserResponse getCurrentUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));
        return userMapper.toResponse(user);
    }

    @Transactional
    public void logout(Long userId, String accessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        user.setOnline(false);
        user.setLastSeen(Instant.now());
        userRepository.save(user);

        Date expiration = tokenService.getExpirationFromToken(accessToken);
        InvalidatedToken invalidatedToken = new InvalidatedToken(accessToken, expiration.toInstant());
        invalidatedTokenRepository.save(invalidatedToken);

        String tokenId = tokenService.getTokenId(accessToken);
        refreshTokenRepository.findByAccessTokenId(tokenId).ifPresent(refreshToken -> {
            refreshToken.revoke();
            refreshTokenRepository.update(refreshToken);
        });
    }

    @Transactional
    public void logoutAllDevices(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        user.setOnline(false);
        user.setLastSeen(Instant.now());
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUser(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findValidToken(refreshTokenString)
                .orElseThrow(() -> BusinessException.unauthorized("Invalid or expired refresh token"));

        User user = refreshToken.getUser();

        String oldAccessToken = refreshToken.getAccessToken();
        if (oldAccessToken != null && !oldAccessToken.isEmpty()) {
            try {
                Date expiration = tokenService.getExpirationFromToken(oldAccessToken);
                if (expiration.after(new Date())) {
                    InvalidatedToken invalidatedToken = new InvalidatedToken(oldAccessToken, expiration.toInstant());
                    invalidatedTokenRepository.save(invalidatedToken);
                }
            } catch (Exception e) {
                // Token đã hết hạn hoặc invalid, bỏ qua
            }
        }

        refreshToken.revoke();
        refreshTokenRepository.update(refreshToken);

        String jti = UUID.randomUUID().toString();
        String newAccessToken = tokenService.generateAccessToken(user, jti);
        RefreshToken newRefreshToken = tokenService.generateRefreshToken(user, jti, newAccessToken);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken.getToken(),
                tokenService.getAccessTokenValidityInSeconds()
        );
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        if (!BCrypt.checkpw(request.getCurrentPassword(), user.getPassword())) {
            throw BusinessException.badRequest("Current password is incorrect", "BAD_REQUEST");
        }

        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw BusinessException.badRequest("New password must be different from current password", "BAD_REQUEST");
        }

        String hashedPassword = BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        userRepository.save(user);
    }
}






