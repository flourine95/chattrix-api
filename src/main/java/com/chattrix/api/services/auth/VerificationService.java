package com.chattrix.api.services.auth;

import com.chattrix.api.enums.TokenType;

import com.chattrix.api.entities.User;
import com.chattrix.api.entities.UserToken;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.repositories.UserTokenRepository;
import com.chattrix.api.requests.ForgotPasswordRequest;
import com.chattrix.api.requests.ResetPasswordRequest;
import com.chattrix.api.requests.VerifyEmailRequest;
import com.chattrix.api.services.notification.EmailService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class VerificationService {

    private static final int TOKEN_EXPIRY_MINUTES = 15;

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserTokenRepository userTokenRepository;

    @Inject
    private EmailService emailService;

    /**
     * Send verification email
     * Flow: Find user -> Validate -> Delete old tokens -> Generate OTP -> Save token -> Send email
     */
    @Transactional
    public void sendVerificationEmailByEmail(String email) {
        // 1. Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.notFound(
                    "User not found with email: " + email, 
                    "RESOURCE_NOT_FOUND"
                ));

        // 2. Validate email not already verified
        if (user.isEmailVerified()) {
            throw BusinessException.badRequest("Email is already verified", "BAD_REQUEST");
        }

        // 3. Delete old verification tokens for this user
        userTokenRepository.deleteByUserIdAndType(user.getId(), TokenType.VERIFY);

        // 4. Generate new OTP
        String otp = emailService.generateOTP();

        // 5. Create verification token
        UserToken token = UserToken.builder()
                .token(otp)
                .user(user)
                .type(TokenType.VERIFY)
                .expiresAt(Instant.now().plus(TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .build();
        userTokenRepository.save(token);

        // 6. Send email
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), otp);
    }

    /**
     * Verify email with OTP
     * Flow: Find user -> Find token -> Validate -> Mark as used -> Update user
     */
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        // 1. Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> BusinessException.notFound(
                    "User not found with email: " + request.getEmail(), 
                    "RESOURCE_NOT_FOUND"
                ));

        // 2. Validate email not already verified
        if (user.isEmailVerified()) {
            throw BusinessException.badRequest("Email is already verified", "BAD_REQUEST");
        }

        // 3. Find token by OTP and type
        UserToken token = userTokenRepository.findByTokenAndType(
                request.getOtp(), 
                TokenType.VERIFY
            )
            .orElseThrow(() -> BusinessException.badRequest(
                "Invalid verification code", 
                "BAD_REQUEST"
            ));

        // 4. Validate token belongs to user
        if (!token.getUser().getId().equals(user.getId())) {
            throw BusinessException.badRequest("Invalid verification code", "BAD_REQUEST");
        }

        // 5. Validate token is valid
        if (!token.isValid()) {
            throw BusinessException.badRequest(
                "Verification code has expired or already been used", 
                "BAD_REQUEST"
            );
        }

        // 6. Mark token as used
        // token.markAsUsed(); // TODO: Implement markAsUsed method
        userTokenRepository.save(token);

        // 7. Mark email as verified
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    /**
     * Send password reset email
     * Flow: Find user -> Delete old tokens -> Generate OTP -> Save token -> Send email
     */
    @Transactional
    public void sendPasswordResetEmail(ForgotPasswordRequest request) {
        // 1. Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> BusinessException.notFound(
                    "User not found with email: " + request.getEmail(), 
                    "RESOURCE_NOT_FOUND"
                ));

        // 2. Delete old password reset tokens for this user
        userTokenRepository.deleteByUserIdAndType(user.getId(), TokenType.RESET);

        // 3. Generate new OTP
        String otp = emailService.generateOTP();

        // 4. Create password reset token
        UserToken token = UserToken.builder()
                .token(otp)
                .user(user)
                .type(TokenType.RESET)
                .expiresAt(Instant.now().plus(TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .build();
        userTokenRepository.save(token);

        // 5. Send email
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), otp);
    }

    /**
     * Reset password with OTP
     * Flow: Find user -> Find token -> Validate -> Mark as used -> Update password
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 1. Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> BusinessException.notFound(
                    "User not found with email: " + request.getEmail(), 
                    "RESOURCE_NOT_FOUND"
                ));

        // 2. Find token by OTP and type
        UserToken token = userTokenRepository.findByTokenAndType(
                request.getOtp(), 
                TokenType.RESET
            )
            .orElseThrow(() -> BusinessException.badRequest(
                "Invalid reset code", 
                "BAD_REQUEST"
            ));

        // 3. Validate token belongs to user
        if (!token.getUser().getId().equals(user.getId())) {
            throw BusinessException.badRequest("Invalid reset code", "BAD_REQUEST");
        }

        // 4. Validate token is valid
        if (!token.isValid()) {
            throw BusinessException.badRequest(
                "Reset code has expired or already been used", 
                "BAD_REQUEST"
            );
        }

        // 5. Mark token as used
        // token.markAsUsed(); // TODO: Implement markAsUsed method
        userTokenRepository.save(token);

        // 6. Hash and update password
        String hashedPassword = BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        userRepository.save(user);
    }

    /**
     * Cleanup expired tokens (scheduled job)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        userTokenRepository.deleteExpiredTokens();
    }
}








