package com.chattrix.chattrixapi.exception;

import jakarta.data.exceptions.DataException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.Map;

@Provider
public class DataExceptionMapper implements ExceptionMapper<DataException> {
    @Override
    public Response toResponse(DataException ex) {
        Throwable cause = ex.getCause();
        String message = cause != null ? cause.getMessage() : ex.getMessage();

        Map<String, Object> error = Map.of(
                "field", detectField(message),
                "message", message
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("errors", List.of(error)))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private String detectField(String message) {
        if (message == null) return "unknown";
        if (message.contains("users_username_key")) return "username";
        if (message.contains("users_email_key")) return "email";
        return "unknown";
    }
}
