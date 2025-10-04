package com.chattrix.chattrixapi.validation;

import com.chattrix.chattrixapi.exception.ValidationError;
import com.chattrix.chattrixapi.exception.ValidationException;
import com.chattrix.chattrixapi.repository.UserRepository;
import com.chattrix.chattrixapi.request.UserCreateRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class UserCreateValidator {

    @Inject
    UserRepository userRepository;

    public void validate(UserCreateRequest req) {
        List<ValidationError> errors = new ArrayList<>();

        if (req.getUsername() == null || req.getUsername().isBlank()) {
            errors.add(new ValidationError("username", "Username is required"));
        } else if (userRepository.usernameExists(req.getUsername())) {
            errors.add(new ValidationError("username", "Username already exists"));
        }

        if (req.getPassword() == null || req.getPassword().length() < 6) {
            errors.add(new ValidationError("password", "Password must be at least 6 characters"));
        }

        if (req.getEmail() == null || !req.getEmail().contains("@")) {
            errors.add(new ValidationError("email", "Invalid email format"));
        } else if (userRepository.emailExists(req.getEmail())) {
            errors.add(new ValidationError("email", "Email already registered"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
