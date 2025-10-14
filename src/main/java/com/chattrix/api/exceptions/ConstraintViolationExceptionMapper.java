package com.chattrix.api.exceptions;

import com.chattrix.api.dto.responses.ApiResponse;
import com.chattrix.api.dto.responses.ErrorDetail;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.stream.Collectors;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ErrorDetail> errors = exception.getConstraintViolations().stream()
                .map(this::mapConstraintViolation)
                .collect(Collectors.toList());

        ApiResponse<Void> response = ApiResponse.validationError("Validation failed", errors);

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

