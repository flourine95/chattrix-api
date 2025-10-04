package com.chattrix.chattrixapi.exception;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Map;

@Provider
public class DatabaseConstraintExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    @Override
    public Response toResponse(ConstraintViolationException ex) {
        String constraint = ex.getConstraintName() != null ? ex.getConstraintName() : "<unknown>";
        String message = ex.getSQLException() != null
                ? ex.getSQLException().getMessage()
                : "Database constraint violation";

        // map constraint name → field name
        String field = mapConstraintToField(constraint);

        Map<String, Object> error = Map.of(
                "field", field,
                "message", message
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("errors", List.of(error)))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private String mapConstraintToField(String constraint) {
        if (constraint.contains("username")) return "username";
        if (constraint.contains("email")) return "email";
        return "unknown";
    }
}
