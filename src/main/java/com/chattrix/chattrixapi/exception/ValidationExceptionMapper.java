package com.chattrix.chattrixapi.exception;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;

import java.util.Map;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {
    @Override
    public Response toResponse(ValidationException ex) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of(
                        "errors", ex.getErrors()
                ))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
