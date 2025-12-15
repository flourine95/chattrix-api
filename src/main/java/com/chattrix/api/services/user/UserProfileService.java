package com.chattrix.api.services.user;

import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.UpdateUserProfileRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserProfileService {

    @Inject
    private UserRepository userRepository;

    public User getUserProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found with id: " + userId, "RESOURCE_NOT_FOUND"));
    }

    @Transactional
    public User updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found with id: " + userId, "RESOURCE_NOT_FOUND"));

        // Validate và cập nhật username
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equals(user.getUsername())) {
                // Kiểm tra username đã tồn tại chưa
                if (userRepository.existsUsernameExcludingUser(newUsername, userId)) {
                    throw BusinessException.conflict("Username already exists", "CONFLICT");
                }
                user.setUsername(newUsername);
            }
        }

        // Validate và cập nhật email
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equals(user.getEmail())) {
                // Kiểm tra email đã tồn tại chưa
                if (userRepository.existsEmailExcludingUser(newEmail, userId)) {
                    throw BusinessException.conflict("Email already exists", "CONFLICT");
                }
                user.setEmail(newEmail);
                // Khi đổi email, cần xác thực lại
                user.setEmailVerified(false);
            }
        }

        // Cập nhật các trường khác
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        if (request.getLocation() != null) {
            user.setLocation(request.getLocation().trim());
        }

        if (request.getProfileVisibility() != null) {
            user.setProfileVisibility(request.getProfileVisibility());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        return userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("User not found with username: " + username, "RESOURCE_NOT_FOUND"));
    }
}





