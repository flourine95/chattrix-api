package com.chattrix.api.exceptions;

/**
 * Exception thrown when a user exceeds the rate limit for an operation.
 */
public class RateLimitExceededException extends BusinessException {
    
    private final int retryAfterSeconds;
    
    public RateLimitExceededException(String message, int retryAfterSeconds) {
        super(message, "RATE_LIMIT_EXCEEDED");
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
