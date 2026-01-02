package com.chattrix.api.exceptions;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public BusinessException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(message, "BAD_REQUEST", 400);
    }

    public static BusinessException badRequest(String message, String code) {
        return new BusinessException(message, code, 400);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(message, "UNAUTHORIZED", 401);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(message, "FORBIDDEN", 403);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(message, "RESOURCE_NOT_FOUND", 404);
    }

    public static BusinessException notFound(String message, String code) {
        return new BusinessException(message, code, 404);
    }

    public static BusinessException conflict(String message, String code) {
        return new BusinessException(message, code, 409);
    }

    public static BusinessException tooManyRequests(String message) {
        return new BusinessException(message, "RATE_LIMIT_EXCEEDED", 429);
    }

    public static BusinessException internalError(String message, String code) {
        return new BusinessException(message, code, 500);
    }
}

