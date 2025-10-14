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

        String jti = UUID.randomUUID().toString();
        String accessToken = tokenService.generateAccessToken(user, jti);
        RefreshToken refreshToken = tokenService.generateRefreshToken(user, jti, accessToken);

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
    public void logoutAllDevices(String username) {
        User user = userRepository.findByUsername(username)
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
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
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
