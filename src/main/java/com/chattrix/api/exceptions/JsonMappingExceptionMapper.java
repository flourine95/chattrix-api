package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ApiResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {

    private static final Logger LOGGER = Logger.getLogger(JsonMappingExceptionMapper.class.getName());

    @Override
    public Response toResponse(JsonMappingException exception) {
        LOGGER.log(Level.SEVERE, "JsonMappingException caught: " + exception.getMessage(), exception);

        ApiResponse<Void> response = ApiResponse.error(
                "Invalid JSON structure — field mapping failed",
                "JSON_MAPPING_ERROR"
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(response)
                .build();
    }
}

