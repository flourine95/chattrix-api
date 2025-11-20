package com.chattrix.api.filters;

import com.chattrix.api.exceptions.RateLimitExceededException;
import com.chattrix.api.services.RateLimitService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Filter for applying per-user rate limiting to call-related endpoints.
 * Uses Guava RateLimiter for smooth rate limiting.
 */
@Provider
@CallRateLimited(operation = CallRateLimited.OperationType.TOKEN_GENERATION)
@Priority(Priorities.USER - 100)
public class CallRateLimitFilter implements ContainerRequestFilter {
    
    @Inject
    private RateLimitService rateLimitService;
    
    @Context
    private ResourceInfo resourceInfo;
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Method method = resourceInfo.getResourceMethod();
        if (method == null) {
            return;
        }
        
        CallRateLimited annotation = method.getAnnotation(CallRateLimited.class);
        if (annotation == null) {
            // Check class-level annotation
            Class<?> resourceClass = resourceInfo.getResourceClass();
            annotation = resourceClass.getAnnotation(CallRateLimited.class);
        }
        
        if (annotation == null) {
            return;
        }
        
        // Get user ID from security context
        UserPrincipal userPrincipal = (UserPrincipal) requestContext.getSecurityContext().getUserPrincipal();
        if (userPrincipal == null) {
            // If not authenticated, skip rate limiting (authentication filter will handle)
            return;
        }
        
        String userId = String.valueOf(userPrincipal.getUserId());
        String operation = getOperationName(annotation.operation());
        
        // Try to acquire a permit
        if (!rateLimitService.tryAcquire(userId, operation)) {
            int retryAfterSeconds = 60; // 1 minute window
            throw new RateLimitExceededException(
                    "Too many requests. Please try again in " + retryAfterSeconds + " seconds.",
                    retryAfterSeconds
            );
        }
    }
    
    /**
     * Converts operation type to operation name string.
     */
    private String getOperationName(CallRateLimited.OperationType operationType) {
        switch (operationType) {
            case TOKEN_GENERATION:
                return "token-generation";
            case CALL_INITIATION:
                return "call-initiation";
            case CALL_HISTORY:
                return "call-history";
            default:
                return "unknown";
        }
    }
}
