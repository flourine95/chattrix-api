package com.chattrix.api.services;

import com.chattrix.api.dto.responses.ErrorDetail;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ValidationService {

    public List<ErrorDetail> validateUsername(String username, boolean checkExists) {
        List<ErrorDetail> errors = new ArrayList<>();

        if (username == null || username.trim().isEmpty()) {
            errors.add(new ErrorDetail("username", "REQUIRED", "Username is required"));
            return errors;
        }

        String trimmedUsername = username.trim();

        if (trimmedUsername.length() < 3 || trimmedUsername.length() > 50) {
            errors.add(new ErrorDetail("username", "INVALID_LENGTH", "Username must be between 3 and 50 characters"));
        }

        if (!trimmedUsername.matches("^[a-zA-Z0-9_]+$")) {
            errors.add(new ErrorDetail("username", "INVALID_FORMAT", "Username can only contain letters, numbers and underscores"));
        }

        return errors;
    }

    public List<ErrorDetail> validatePassword(String password, String fieldName) {
        List<ErrorDetail> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add(new ErrorDetail(fieldName, "REQUIRED", getPasswordRequiredMessage(fieldName)));
            return errors;
        }

        if (password.length() < 6 || password.length() > 100) {
            errors.add(new ErrorDetail(fieldName, "INVALID_LENGTH", getPasswordLengthMessage(fieldName)));
        }

        return errors;
    }

    public List<ErrorDetail> validateDisplayName(String displayName) {
        List<ErrorDetail> errors = new ArrayList<>();

        if (displayName == null || displayName.trim().isEmpty()) {
            errors.add(new ErrorDetail("displayName", "REQUIRED", "Display name is required"));
            return errors;
        }

        if (displayName.trim().length() > 100) {
            errors.add(new ErrorDetail("displayName", "INVALID_LENGTH", "Display name must not exceed 100 characters"));
        }

        return errors;
    }

    private String getPasswordRequiredMessage(String fieldName) {
        return switch (fieldName) {
            case "password" -> "Password is required";
            case "currentPassword" -> "Current password is required";
            case "newPassword" -> "New password is required";
            default -> "Password is required";
        };
    }

    private String getPasswordLengthMessage(String fieldName) {
        return switch (fieldName) {
            case "newPassword" -> "New password must be between 6 and 100 characters";
            default -> "Password must be between 6 and 100 characters";
        };
    }
}

