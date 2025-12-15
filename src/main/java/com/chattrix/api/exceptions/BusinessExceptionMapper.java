package com.chattrix.api.exceptions;

import com.chattrix.api.responses.ApiResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@Provider
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {

    @Override
    public Response toResponse(BusinessException ex) {
        String requestId = UUID.randomUUID().toString();

        log.warn("Business error [{}]: {}", requestId, ex.getMessage());

        return Response.status(ex.getHttpStatus())
                .entity(ApiResponse.error(ex.getErrorCode(), ex.getMessage(), requestId))
                .build();
    }
}

