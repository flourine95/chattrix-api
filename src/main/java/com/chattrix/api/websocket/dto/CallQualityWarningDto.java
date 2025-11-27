package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallQualityWarningDto {
    private String callId;
    private Long userId;
    private String message;
}