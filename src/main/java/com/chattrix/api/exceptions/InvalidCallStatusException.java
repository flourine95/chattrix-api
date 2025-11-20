package com.chattrix.api.exceptions;

public class InvalidCallStatusException extends BadRequestException {
    public InvalidCallStatusException(String message) {
        super(message, "INVALID_CALL_STATUS");
    }

    public InvalidCallStatusException(String callId, String currentStatus, String expectedStatus) {
        super(String.format("Invalid call status for call %s. Current: %s, Expected: %s", 
            callId, currentStatus, expectedStatus), "INVALID_CALL_STATUS");
    }
}
