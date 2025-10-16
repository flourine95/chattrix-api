package com.chattrix.api.services;

import com.chattrix.api.entities.PasswordResetToken;
import com.chattrix.api.entities.User;
import com.chattrix.api.entities.VerificationToken;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.repositories.PasswordResetTokenRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.repositories.VerificationTokenRepository;
import com.chattrix.api.requests.ForgotPasswordRequest;
import com.chattrix.api.requests.ResendVerificationRequest;
import com.chattrix.api.requests.ResetPasswordRequest;
import com.chattrix.api.requests.VerifyEmailRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class VerificationService {

    private static final int VERIFICATION_TOKEN_EXPIRY_MINUTES = 15;
    private static final int PASSWORD_RESET_TOKEN_EXPIRY_MINUTES = 15;
    @Inject
    private UserRepository userRepository;
    @Inject
    private VerificationTokenRepository verificationTokenRepository;
    @Inject
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Inject
    private EmailService emailService;

    @Transactional
    public void sendVerificationEmail(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        // Delete old verification tokens for this user
        verificationTokenRepository.deleteByUser(user);

        // Generate new OTP
        String otp = emailService.generateOTP();

        // Create verification token
        VerificationToken token = new VerificationToken();
        token.setToken(otp);
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(VERIFICATION_TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        verificationTokenRepository.save(token);

        // Send email
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), otp);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        VerificationToken token = verificationTokenRepository.findByToken(request.getOtp())
                .orElseThrow(() -> new BadRequestException("Invalid verification code"));

        if (!token.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Invalid verification code");
        }

        if (!token.isValid()) {
            throw new BadRequestException("Verification code has expired or already been used");
        }

        // Mark token as used
        token.markAsUsed();
        verificationTokenRepository.save(token);

        // Mark email as verified
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void sendPasswordResetEmail(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        // Delete old password reset tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Generate new OTP
        String otp = emailService.generateOTP();

        // Create password reset token
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(otp);
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(PASSWORD_RESET_TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(token);

        // Send email
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getOtp())
                .orElseThrow(() -> new BadRequestException("Invalid reset code"));

        if (!token.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Invalid reset code");
        }

        if (!token.isValid()) {
            throw new BadRequestException("Reset code has expired or already been used");
        }

        // Mark token as used
        token.markAsUsed();
        passwordResetTokenRepository.save(token);

        // Update password
        String hashedPassword = BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        userRepository.save(user);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        verificationTokenRepository.deleteExpiredTokens();
        passwordResetTokenRepository.deleteExpiredTokens();
    }
}
