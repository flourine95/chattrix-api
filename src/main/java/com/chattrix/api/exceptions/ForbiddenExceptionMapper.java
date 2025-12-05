package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ApiResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

    @Override
    public Response toResponse(ForbiddenException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(ApiResponse.error(exception.getMessage(), exception.getErrorCode()))
                .build();
    }
}

