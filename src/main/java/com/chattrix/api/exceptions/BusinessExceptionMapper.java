package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {

    private static final Logger LOGGER = Logger.getLogger(BusinessExceptionMapper.class.getName());

    @Override
    public Response toResponse(BusinessException exception) {
        String requestId = UUID.randomUUID().toString();
        
        // Log with appropriate level based on status
        Response.Status status = determineHttpStatus(exception);
        if (status.getStatusCode() >= 500) {
            LOGGER.log(Level.SEVERE, "Server error [requestId: {0}]: {1}", 
                new Object[]{requestId, exception.getMessage()});
        } else if (status.getStatusCode() == 401 || status.getStatusCode() == 403) {
            LOGGER.log(Level.WARNING, "Security error [requestId: {0}]: {1}", 
                new Object[]{requestId, exception.getMessage()});
        } else {
            LOGGER.log(Level.INFO, "Business exception [requestId: {0}]: {1}", 
                new Object[]{requestId, exception.getMessage()});
        }

        ErrorResponse response = ErrorResponse.of(
                exception.getErrorCode(),
                exception.getMessage(),
                requestId
        );

        return Response.status(status)
                .entity(response)
                .build();
    }

    private Response.Status determineHttpStatus(BusinessException exception) {
        return switch (exception.getErrorCode()) {
            // Authentication/Authorization errors
            case "UNAUTHORIZED", "AUTHENTICATION_FAILED" -> Response.Status.UNAUTHORIZED;
            case "UNAUTHORIZED_ACCESS", "NOT_CONTACTS" -> Response.Status.FORBIDDEN;
            
            // Not Found errors
            case "NOT_FOUND", "CALL_NOT_FOUND", "USER_NOT_FOUND" -> Response.Status.NOT_FOUND;
            
            // Conflict errors
            case "CONFLICT", "USER_BUSY" -> Response.Status.CONFLICT;
            
            // Rate Limiting
            case "RATE_LIMIT_EXCEEDED" -> Response.Status.TOO_MANY_REQUESTS;
            
            // Server errors
            case "TOKEN_GENERATION_FAILED", "TOKEN_REFRESH_FAILED", "INTERNAL_SERVER_ERROR" -> 
                Response.Status.INTERNAL_SERVER_ERROR;
            
            // Bad Request errors (default for most validation and business logic errors)
            case "BAD_REQUEST", "NO_ACTIVE_CALL", "CALL_TIMEOUT", "CALL_ALREADY_ENDED",
                 "INVALID_CALL_STATUS", "INVALID_STATUS_TRANSITION", "INVALID_CALL_TYPE",
                 "INVALID_CHANNEL_ID", "INVALID_ROLE", "INVALID_EXPIRATION", "INVALID_QUALITY",
                 "INVALID_PACKET_LOSS", "INVALID_RTT", "INVALID_REASON", "VALIDATION_ERROR" -> 
                Response.Status.BAD_REQUEST;
            
            default -> Response.Status.BAD_REQUEST;
        };
    }
}

