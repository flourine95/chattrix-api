package com.chattrix.api.exceptions;

public class BadRequestException extends BusinessException {
    public BadRequestException(String message) {
        super(message, "BAD_REQUEST");
    }

    public BadRequestException(String message, String errorCode) {
        super(message, errorCode);
    }
}

