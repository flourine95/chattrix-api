package com.chattrix.api.responses;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallConnectionResponse {
    private CallResponse callInfo;

    private String token;
}
