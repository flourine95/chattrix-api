package com.chattrix.api.exceptions;

public class UserBusyException extends ConflictException {
    public UserBusyException(String userId) {
        super("User is already in a call: " + userId, "USER_BUSY");
    }
}
