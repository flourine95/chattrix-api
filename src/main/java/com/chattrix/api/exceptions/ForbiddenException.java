package com.chattrix.api.exceptions;

public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN");
    }
    
    public ForbiddenException(String message, String errorCode) {
        super(message, errorCode);
    }
}

