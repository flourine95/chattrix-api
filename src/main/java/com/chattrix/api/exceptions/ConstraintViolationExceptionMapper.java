package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.ErrorDetail;
import com.chattrix.api.responses.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOGGER = Logger.getLogger(ConstraintViolationExceptionMapper.class.getName());

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String requestId = UUID.randomUUID().toString();
        
        List<ErrorDetail> errors = exception.getConstraintViolations().stream()
                .map(this::mapConstraintViolation)
                .collect(Collectors.toList());

        // Build detailed validation error message
        String detailedMessage = errors.stream()
                .map(e -> e.getField() + ": " + e.getMessage())
                .collect(Collectors.joining(", "));

        LOGGER.log(Level.INFO, "Validation failed [requestId: {0}]: {1}", 
            new Object[]{requestId, detailedMessage});

        ErrorResponse response = ErrorResponse.of(
                "VALIDATION_ERROR",
                "Request validation failed: " + detailedMessage,
                requestId
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(response)
                .build();
    }

    private ErrorDetail mapConstraintViolation(ConstraintViolation<?> violation) {
        String fieldName = extractFieldName(violation.getPropertyPath().toString());
        String annotationName = violation.getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();

        return ErrorDetail.builder()
                .field(fieldName)
                .code(annotationName)
                .message(violation.getMessage())
                .build();
    }

    private String extractFieldName(String propertyPath) {
        String[] parts = propertyPath.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : propertyPath;
    }
}

