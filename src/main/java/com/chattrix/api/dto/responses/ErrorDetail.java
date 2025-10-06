package com.chattrix.api.dto.responses;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorDetail {
    private String field;
    private String code;
    private String message;
}
