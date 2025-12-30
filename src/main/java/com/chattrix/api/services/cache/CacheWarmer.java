package com.chattrix.api.services.cache;

import com.chattrix.api.entities.User;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.UserResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to warm up caches on application startup or on-demand
 */
@ApplicationScoped
public class CacheWarmer {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheWarmer.class);
    private static final int HOT_USERS_LIMIT = 1000;
    
    @Inject
    private UserRepository userRepository;
    
    @Inject
    private UserMapper userMapper;
    
    @Inject
    private UserProfileCache userProfileCache;
    
    /**
     * Warm up user profile cache with hot users
     * Hot users = users with recent activity
     */
    public void warmUpUserProfiles() {
        logger.info("Warming up user profile cache...");
        
        try {
            // Get hot users (users with recent lastSeen)
            List<User> hotUsers = userRepository.findRecentActiveUsers(HOT_USERS_LIMIT);
            
            // Convert to UserResponse and cache
            Map<Long, UserResponse> userProfiles = hotUsers.stream()
                .collect(Collectors.toMap(
                    User::getId,
                    userMapper::toResponse
                ));
            
            userProfileCache.putAll(userProfiles);
            
            logger.info("User profile cache warmed up with {} users", userProfiles.size());
        } catch (Exception e) {
            logger.error("Error warming up user profile cache", e);
        }
    }
    
    /**
     * Warm up all caches
     */
    public void warmUpAll() {
        logger.info("Starting cache warm-up...");
        warmUpUserProfiles();
        // Add more warm-up methods as needed
        logger.info("Cache warm-up completed");
    }
}
