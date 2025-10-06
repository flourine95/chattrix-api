package com.chattrix.api.dto.responses;

import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<ErrorDetail> errors;

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Request was successful.");
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        ErrorDetail errorDetail = new ErrorDetail(null, errorCode, message);
        return new ApiResponse<>(false, message, null, Collections.singletonList(errorDetail));
    }

    public static <T> ApiResponse<T> validationError(String message, List<ErrorDetail> errors) {
        return new ApiResponse<>(false, message, null, errors);
    }
}
