package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ApiResponse;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.NotFoundException;
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

        // Handle NotFoundException (404 errors including path parameter issues)
        if (exception instanceof NotFoundException) {
            String message = exception.getMessage();

            // Check if it's a path parameter extraction error
            if (message != null && message.contains("Unable to extract parameter")) {
                // Extract parameter name if possible
                String paramName = extractParameterName(message);
                message = paramName != null
                        ? "Invalid or missing path parameter: " + paramName
                        : "Invalid request path or missing required parameter";

                ApiResponse<Void> response = ApiResponse.error(message, "INVALID_PATH_PARAMETER");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(response)
                        .build();
            }

            // Regular 404 - resource not found
            ApiResponse<Void> response = ApiResponse.error(
                    "Resource not found",
                    "RESOURCE_NOT_FOUND"
            );
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(response)
                    .build();
        }

        // Handle other WebApplicationException
        if (exception instanceof WebApplicationException webEx) {
            if (webEx.getResponse().hasEntity()) {
                return webEx.getResponse();
            }

            String message = exception.getMessage() != null ? exception.getMessage() : "Request failed";
            ApiResponse<Void> response = ApiResponse.error(message, "REQUEST_ERROR");
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

    /**
     * Extract parameter name from error message like:
     * "RESTEASY003870: Unable to extract parameter from http request: jakarta.ws.rs.PathParam("conversationId") value is 'null'"
     */
    private String extractParameterName(String message) {
        if (message == null) return null;

        // Try to extract parameter name from PathParam or QueryParam
        int start = message.indexOf("PathParam(\"");
        if (start == -1) {
            start = message.indexOf("QueryParam(\"");
        }

        if (start != -1) {
            start += 11; // Length of 'PathParam("' or 'QueryParam("'
            int end = message.indexOf("\")", start);
            if (end != -1) {
                return message.substring(start, end);
            }
        }

        return null;
    }
}
