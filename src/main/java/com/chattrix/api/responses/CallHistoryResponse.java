package com.chattrix.api.responses;

import com.chattrix.api.entities.CallDirection;
import com.chattrix.api.entities.CallHistoryStatus;
import com.chattrix.api.entities.CallType;
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
    private CallHistoryStatus status;
    private CallDirection direction;
    private Instant timestamp;
    private Integer durationSeconds;
}
