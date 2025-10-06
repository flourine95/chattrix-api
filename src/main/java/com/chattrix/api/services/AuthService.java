package com.chattrix.api.services;

import com.chattrix.api.dto.requests.LoginRequest;
import com.chattrix.api.dto.requests.RegisterRequest;
import com.chattrix.api.dto.responses.AuthResponse;
import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import org.mindrot.jbcrypt.BCrypt;

@ApplicationScoped
public class AuthService {

    @Inject
    private UserRepository userRepository;

    @Inject
    private TokenService tokenService;

    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (userRepository.existsUsername(registerRequest.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setDisplayName(registerRequest.getDisplayName());
        // Hash the password before saving
        String hashedPassword = BCrypt.hashpw(registerRequest.getPassword(), BCrypt.gensalt());
        newUser.setPassword(hashedPassword);

        userRepository.save(newUser);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new NotAuthorizedException("Invalid credentials"));

        if (!BCrypt.checkpw(loginRequest.getPassword(), user.getPassword())) {
            throw new NotAuthorizedException("Invalid credentials");
        }

        String token = tokenService.generateToken(user);
        return new AuthResponse(token);
    }
}

