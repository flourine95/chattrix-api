package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class CallNotFoundExceptionMapper implements ExceptionMapper<CallNotFoundException> {

    private static final Logger LOGGER = Logger.getLogger(CallNotFoundExceptionMapper.class.getName());

    @Override
    public Response toResponse(CallNotFoundException exception) {
        String requestId = UUID.randomUUID().toString();
        
        LOGGER.log(Level.INFO, "Call not found [requestId: {0}]: {1}", 
            new Object[]{requestId, exception.getMessage()});

        ErrorResponse response = ErrorResponse.of(
                "CALL_NOT_FOUND",
                exception.getMessage(),
                requestId
        );

        return Response.status(Response.Status.NOT_FOUND)
                .entity(response)
                .build();
    }
}
