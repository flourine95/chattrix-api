package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class InvalidStatusTransitionExceptionMapper implements ExceptionMapper<InvalidStatusTransitionException> {

    private static final Logger LOGGER = Logger.getLogger(InvalidStatusTransitionExceptionMapper.class.getName());

    @Override
    public Response toResponse(InvalidStatusTransitionException exception) {
        String requestId = UUID.randomUUID().toString();
        
        LOGGER.log(Level.INFO, "Invalid status transition [requestId: {0}]: {1}", 
            new Object[]{requestId, exception.getMessage()});

        ErrorResponse response = ErrorResponse.of(
                "INVALID_STATUS_TRANSITION",
                exception.getMessage(),
                requestId
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(response)
                .build();
    }
}
