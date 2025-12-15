package com.chattrix.api.responses;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String code;
    private Map<String, String> details;
    private String requestId;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Request was successful");
    }

    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .code(code)
                .requestId(requestId)
                .build();
    }

    public static <T> ApiResponse<T> validationError(Map<String, String> fieldErrors, String requestId) {
        return ApiResponse.<T>builder()
                .success(false)
                .message("Validation failed")
                .code("VALIDATION_ERROR")
                .details(fieldErrors)
                .requestId(requestId)
                .build();
    }
}

