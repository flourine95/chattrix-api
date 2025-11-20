package com.chattrix.api.filters;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for applying rate limiting to call-related endpoints.
 * Uses per-user rate limiting with Guava RateLimiter.
 */
@NameBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CallRateLimited {
    
    /**
     * The operation type for rate limiting.
     */
    OperationType operation();
    
    /**
     * Operation types with different rate limits.
     */
    enum OperationType {
        TOKEN_GENERATION,   // 10 requests per minute
        CALL_INITIATION,    // 5 requests per minute
        CALL_HISTORY        // 20 requests per minute
    }
}
