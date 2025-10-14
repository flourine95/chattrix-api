package com.chattrix.api.services;

import com.chattrix.api.dto.requests.ChangePasswordRequest;
import com.chattrix.api.dto.requests.LoginRequest;
import com.chattrix.api.dto.requests.RegisterRequest;
import com.chattrix.api.dto.responses.AuthResponse;
import com.chattrix.api.dto.responses.UserDto;
import com.chattrix.api.entities.InvalidatedToken;
import com.chattrix.api.entities.RefreshToken;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.exceptions.UnauthorizedException;
import com.chattrix.api.repositories.InvalidatedTokenRepository;
import com.chattrix.api.repositories.RefreshTokenRepository;
import com.chattrix.api.repositories.UserRepository;
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

    @Transactional
    public void register(RegisterRequest registerRequest) {
        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername().trim());
        newUser.setDisplayName(registerRequest.getDisplayName().trim());
        String hashedPassword = BCrypt.hashpw(registerRequest.getPassword(), BCrypt.gensalt());
        newUser.setPassword(hashedPassword);
        userRepository.save(newUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!BCrypt.checkpw(loginRequest.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        user.setOnline(true);
        user.setLastSeen(Instant.now());
        userRepository.save(user);

        // Tạo access token và refresh token với linking
        String jti = UUID.randomUUID().toString();
        String accessToken = tokenService.generateAccessToken(user, jti);
        RefreshToken refreshToken = tokenService.generateRefreshToken(user, jti);

        return new AuthResponse(
            accessToken,
            refreshToken.getToken(),
            tokenService.getAccessTokenValidityInSeconds()
        );
    }

    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserDto.fromUser(user);
    }

    @Transactional
    public void logout(String username, String accessToken) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setOnline(false);
        user.setLastSeen(Instant.now());
        userRepository.save(user);

        // Thêm access token vào blacklist
        Date expiration = tokenService.getExpirationFromToken(accessToken);
        InvalidatedToken invalidatedToken = new InvalidatedToken(accessToken, expiration.toInstant());
        invalidatedTokenRepository.save(invalidatedToken);

        // Chỉ revoke refresh token tương ứng với access token này (single device logout)
        String tokenId = tokenService.getTokenId(accessToken);
        refreshTokenRepository.findByAccessTokenId(tokenId).ifPresent(refreshToken -> {
            refreshToken.revoke();
            refreshTokenRepository.update(refreshToken);
        });
    }

    @Transactional
    public void logoutAllDevices(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setOnline(false);
        user.setLastSeen(Instant.now());
        userRepository.save(user);

        // Revoke tất cả refresh tokens của user
        refreshTokenRepository.revokeAllByUser(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        // Tìm refresh token trong database
        RefreshToken refreshToken = refreshTokenRepository.findValidToken(refreshTokenString)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        User user = refreshToken.getUser();

        // Revoke refresh token cũ (rotation)
        refreshToken.revoke();
        refreshTokenRepository.update(refreshToken);

        // Tạo access token và refresh token mới
        String jti = UUID.randomUUID().toString();
        String newAccessToken = tokenService.generateAccessToken(user, jti);
        RefreshToken newRefreshToken = tokenService.generateRefreshToken(user, jti);

        return new AuthResponse(
            newAccessToken,
            newRefreshToken.getToken(),
            tokenService.getAccessTokenValidityInSeconds()
        );
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        // Bean Validation đã xử lý NotBlank và Size cho passwords
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!BCrypt.checkpw(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Check if new password is same as current
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        String hashedPassword = BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        userRepository.save(user);
    }
}
