package com.chattrix.api.exceptions;

public class CallTimeoutException extends BadRequestException {
    public CallTimeoutException(String callId) {
        super("Call has timed out: " + callId, "CALL_TIMEOUT");
    }
}
