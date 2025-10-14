package com.chattrix.api.exceptions;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super(message, "NOT_FOUND");
    }
}

