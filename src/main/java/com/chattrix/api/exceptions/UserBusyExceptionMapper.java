package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class UserBusyExceptionMapper implements ExceptionMapper<UserBusyException> {

    private static final Logger LOGGER = Logger.getLogger(UserBusyExceptionMapper.class.getName());

    @Override
    public Response toResponse(UserBusyException exception) {
        String requestId = UUID.randomUUID().toString();
        
        LOGGER.log(Level.INFO, "User busy [requestId: {0}]: {1}", 
            new Object[]{requestId, exception.getMessage()});

        ErrorResponse response = ErrorResponse.of(
                "USER_BUSY",
                exception.getMessage(),
                requestId
        );

        return Response.status(Response.Status.CONFLICT)
                .entity(response)
                .build();
    }
}
