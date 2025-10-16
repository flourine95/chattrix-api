package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ApiResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {

    private static final Logger LOGGER = Logger.getLogger(BusinessExceptionMapper.class.getName());

    @Override
    public Response toResponse(BusinessException exception) {
        LOGGER.log(Level.INFO, "Business exception: {0}", exception.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                exception.getMessage(),
                exception.getErrorCode()
        );

        Response.Status status = determineHttpStatus(exception);

        return Response.status(status)
                .entity(response)
                .build();
    }

    private Response.Status determineHttpStatus(BusinessException exception) {
        return switch (exception.getErrorCode()) {
            case "UNAUTHORIZED" -> Response.Status.UNAUTHORIZED;
            case "NOT_FOUND" -> Response.Status.NOT_FOUND;
            case "BAD_REQUEST" -> Response.Status.BAD_REQUEST;
            default -> Response.Status.BAD_REQUEST;
        };
    }
}

