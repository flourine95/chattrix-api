package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class InvalidCallStatusExceptionMapper implements ExceptionMapper<InvalidCallStatusException> {

    private static final Logger LOGGER = Logger.getLogger(InvalidCallStatusExceptionMapper.class.getName());

    @Override
    public Response toResponse(InvalidCallStatusException exception) {
        String requestId = UUID.randomUUID().toString();
        
        LOGGER.log(Level.INFO, "Invalid call status [requestId: {0}]: {1}", 
            new Object[]{requestId, exception.getMessage()});

        ErrorResponse response = ErrorResponse.of(
                "INVALID_CALL_STATUS",
                exception.getMessage(),
                requestId
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(response)
                .build();
    }
}
