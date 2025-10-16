package com.chattrix.api.services;

import com.chattrix.api.entities.InvalidatedToken;
import com.chattrix.api.entities.RefreshToken;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.exceptions.UnauthorizedException;
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

    @Transactional
    public void register(RegisterRequest registerRequest) {
        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername().trim());
        newUser.setEmail(registerRequest.getEmail().trim());
        newUser.setFullName(registerRequest.getFullName().trim());
        String hashedPassword = BCrypt.hashpw(registerRequest.getPassword(), BCrypt.gensalt());
        newUser.setPassword(hashedPassword);
        userRepository.save(newUser);

        ResendVerificationRequest resendVerificationRequest = new ResendVerificationRequest();
        resendVerificationRequest.setEmail(registerRequest.getEmail());
        verificationService.sendVerificationEmail(resendVerificationRequest);
    }

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid username/email or password"));

        if (!BCrypt.checkpw(loginRequest.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid username/email or password");
        }

        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Email not verified. Please check your email to verify your account.");
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }

    @Transactional
    public void logout(Long userId, String accessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setOnline(false);
        user.setLastSeen(Instant.now());
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUser(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findValidToken(refreshTokenString)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!BCrypt.checkpw(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        String hashedPassword = BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        userRepository.save(user);
    }
}
