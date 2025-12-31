package com.chattrix.api.responses;

import com.chattrix.api.enums.CallDirection;
import com.chattrix.api.enums.CallStatus;
import com.chattrix.api.enums.CallType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallHistoryResponse {
    private String id;
    private String callId;
    private String remoteUserId;
    private String remoteUserName;
    private String remoteUserAvatar;
    private CallType callType;
    private CallStatus status;
    private CallDirection direction;
    private Instant timestamp;
    private Integer durationSeconds;
}
