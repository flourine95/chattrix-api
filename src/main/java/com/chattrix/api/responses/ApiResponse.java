package com.chattrix.api.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private ErrorPayload error;
    private String requestId;

    // --- Success Response ---
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

    // --- Single Error (Business logic, Auth, etc.) ---
    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .requestId(requestId)
                .error(new ErrorPayload(code, message, null))
                .build();
    }

    // --- Validation Error (Multiple field errors) ---
    public static <T> ApiResponse<T> validationError(Map<String, String> fieldErrors, String requestId) {
        return ApiResponse.<T>builder()
                .success(false)
                .message("Validation failed")
                .requestId(requestId)
                .error(new ErrorPayload("VALIDATION_ERROR", "Please check input fields", fieldErrors))
                .build();
    }

    // --- Validation Error (Single field - simplified) ---
    public static <T> ApiResponse<T> validationError(String message, String requestId) {
        return error("VALIDATION_ERROR", message, requestId);
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorPayload {
        private String code;
        private String message;
        private Map<String, String> details; // Field-level errors (optional)

        public ErrorPayload(String code, String message, Map<String, String> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }
    }
}

