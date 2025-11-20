package com.chattrix.api.responses;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private boolean success;
    private ErrorInfo error;
    private String requestId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorInfo {
        private String code;
        private String message;
    }

    public static ErrorResponse of(String errorCode, String message, String requestId) {
        return ErrorResponse.builder()
                .success(false)
                .error(ErrorInfo.builder()
                        .code(errorCode)
                        .message(message)
                        .build())
                .requestId(requestId)
                .build();
    }
}
