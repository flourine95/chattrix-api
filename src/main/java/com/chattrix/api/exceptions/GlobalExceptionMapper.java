package com.chattrix.api.exceptions;

import com.chattrix.api.dto.responses.ApiResponse;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        LOGGER.log(Level.SEVERE, "Exception occurred: " + exception.getMessage(), exception);

        // Handle Jackson JSON parsing errors
        if (exception instanceof JsonParseException) {
            ApiResponse<Void> response = ApiResponse.error(
                    "Malformed JSON - syntax error in request body",
                    "JSON_PARSE_ERROR"
            );
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(response)
                    .build();
        }

        // Handle Jackson JSON mapping errors
        if (exception instanceof JsonMappingException) {
            ApiResponse<Void> response = ApiResponse.error(
                    "Invalid JSON structure - field mapping failed",
                    "JSON_MAPPING_ERROR"
            );
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(response)
                    .build();
        }

        // Handle JSON-B parsing errors
        if (exception instanceof JsonbException) {
            ApiResponse<Void> response = ApiResponse.error(
                    "Invalid JSON format",
                    "JSON_PARSE_ERROR"
            );
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(response)
                    .build();
        }

        // Handle ProcessingException with JsonParseException cause
        if (exception instanceof ProcessingException && exception.getCause() instanceof JsonParseException) {
            ApiResponse<Void> response = ApiResponse.error(
                    "Malformed JSON input",
                    "JSON_PARSE_ERROR"
            );
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(response)
                    .build();
        }

        // Handle WebApplicationException (already has response)
        if (exception instanceof WebApplicationException webEx) {
            if (webEx.getResponse().hasEntity()) {
                return webEx.getResponse();
            }
            ApiResponse<Void> response = ApiResponse.error(
                    exception.getMessage() != null ? exception.getMessage() : "Request failed",
                    "REQUEST_ERROR"
            );
            return Response.status(webEx.getResponse().getStatus())
                    .entity(response)
                    .build();
        }

        // Handle all other unexpected errors
        ApiResponse<Void> response = ApiResponse.error(
                "An unexpected error occurred",
                "INTERNAL_ERROR"
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(response)
                .build();
    }
}
