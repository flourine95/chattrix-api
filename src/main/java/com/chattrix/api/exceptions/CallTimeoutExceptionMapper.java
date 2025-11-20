package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class CallTimeoutExceptionMapper implements ExceptionMapper<CallTimeoutException> {

    private static final Logger LOGGER = Logger.getLogger(CallTimeoutExceptionMapper.class.getName());

    @Override
    public Response toResponse(CallTimeoutException exception) {
        String requestId = UUID.randomUUID().toString();
        
        LOGGER.log(Level.INFO, "Call timeout [requestId: {0}]: {1}", 
            new Object[]{requestId, exception.getMessage()});

        ErrorResponse response = ErrorResponse.of(
                "CALL_TIMEOUT",
                exception.getMessage(),
                requestId
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(response)
                .build();
    }
}
