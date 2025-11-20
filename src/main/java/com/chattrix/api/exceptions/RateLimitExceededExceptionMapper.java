package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;

/**
 * Exception mapper for RateLimitExceededException.
 * Returns HTTP 429 (Too Many Requests) with Retry-After header.
 */
@Provider
public class RateLimitExceededExceptionMapper implements ExceptionMapper<RateLimitExceededException> {
    
    @Override
    public Response toResponse(RateLimitExceededException exception) {
        ErrorResponse errorResponse = ErrorResponse.of(
                exception.getErrorCode(),
                exception.getMessage(),
                UUID.randomUUID().toString()
        );
        
        return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .entity(errorResponse)
                .header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()))
                .build();
    }
}
