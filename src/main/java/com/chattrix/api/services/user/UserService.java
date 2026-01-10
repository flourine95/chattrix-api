package com.chattrix.api.services.user;

import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.UserResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class UserService {
    @Inject
    private UserMapper userMapper;
    @Inject
    private UserRepository userRepository;

    /**
     * Get all users - Optimized with DTO projection
     * No entity mapping, no MapStruct needed
     */
    public List<UserResponse> findAllUsers() {
        return userRepository.findAllAsDTO();
    }
}