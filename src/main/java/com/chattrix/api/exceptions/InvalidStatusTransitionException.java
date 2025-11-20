package com.chattrix.api.exceptions;

public class InvalidStatusTransitionException extends BadRequestException {
    public InvalidStatusTransitionException(String message) {
        super(message, "INVALID_STATUS_TRANSITION");
    }

    public InvalidStatusTransitionException(String currentStatus, String targetStatus) {
        super(String.format("Invalid status transition from %s to %s", currentStatus, targetStatus), 
            "INVALID_STATUS_TRANSITION");
    }
}
