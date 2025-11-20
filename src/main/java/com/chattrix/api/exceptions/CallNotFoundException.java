package com.chattrix.api.exceptions;

public class CallNotFoundException extends ResourceNotFoundException {
    public CallNotFoundException(String callId) {
        super("Call not found with ID: " + callId);
    }
}
