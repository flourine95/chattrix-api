package com.chattrix.api.exceptions;

import com.chattrix.api.dto.responses.ApiResponse;
import com.fasterxml.jackson.core.JsonParseException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    private static final Logger LOGGER = Logger.getLogger(JsonParseExceptionMapper.class.getName());

    @Override
    public Response toResponse(JsonParseException exception) {
        LOGGER.log(Level.SEVERE, "JsonParseException caught: " + exception.getMessage(), exception);

        ApiResponse<Void> response = ApiResponse.error(
                "Malformed JSON request — syntax error",
                "JSON_PARSE_ERROR"
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(response)
                .build();
    }
}

