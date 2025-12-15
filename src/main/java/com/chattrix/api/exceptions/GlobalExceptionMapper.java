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
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable ex) {
        String requestId = UUID.randomUUID().toString();

        // Handle Jackson JSON parsing errors
        if (ex instanceof JsonParseException) {
            log.warn("JSON parse error [{}]: {}", requestId, ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("JSON_PARSE_ERROR", "Malformed JSON - syntax error in request body", requestId))
                    .build();
        }

        // Handle Jackson JSON mapping errors
        if (ex instanceof JsonMappingException) {
            log.warn("JSON mapping error [{}]: {}", requestId, ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("JSON_MAPPING_ERROR", "Invalid JSON structure - field mapping failed", requestId))
                    .build();
        }

        // Handle JSON-B parsing errors
        if (ex instanceof JsonbException) {
            log.warn("JSON-B error [{}]: {}", requestId, ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("JSON_PARSE_ERROR", "Invalid JSON format", requestId))
                    .build();
        }

        // Handle ProcessingException with JsonParseException cause
        if (ex instanceof ProcessingException && ex.getCause() instanceof JsonParseException) {
            log.warn("Processing exception with JSON parse error [{}]: {}", requestId, ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("JSON_PARSE_ERROR", "Malformed JSON input", requestId))
                    .build();
        }

        // Handle NotFoundException (404 errors including path parameter issues)
        if (ex instanceof NotFoundException) {
            String message = ex.getMessage();

            // Check if it's a path parameter extraction error
            if (message != null && message.contains("Unable to extract parameter")) {
                String paramName = extractParameterName(message);
                message = paramName != null
                        ? "Invalid or missing path parameter: " + paramName
                        : "Invalid request path or missing required parameter";

                log.info("Path parameter error [{}]: {}", requestId, message);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ApiResponse.error("INVALID_PATH_PARAMETER", message, requestId))
                        .build();
            }

            // Regular 404 - resource not found
            log.info("Resource not found [{}]: {}", requestId, message);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("RESOURCE_NOT_FOUND", "Resource not found", requestId))
                    .build();
        }

        // Handle other WebApplicationException (framework errors)
        if (ex instanceof WebApplicationException webEx) {
            if (webEx.getResponse().hasEntity()) {
                return webEx.getResponse();
            }

            String message = ex.getMessage() != null ? ex.getMessage() : "Request failed";
            log.warn("Web application error [{}]: {}", requestId, message);
            return Response.status(webEx.getResponse().getStatus())
                    .entity(ApiResponse.error("API_ERROR", message, requestId))
                    .build();
        }

        // Handle all other unexpected errors (500)
        log.error("System error [" + requestId + "]", ex);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("INTERNAL_SERVER_ERROR", "An unexpected error occurred", requestId))
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

