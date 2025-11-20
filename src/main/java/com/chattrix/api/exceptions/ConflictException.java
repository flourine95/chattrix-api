package com.chattrix.api.exceptions;

public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(message, "CONFLICT");
    }

    public ConflictException(String message, String errorCode) {
        super(message, errorCode);
    }
}
