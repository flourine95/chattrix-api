package com.chattrix.api.websocket.dto;

import com.chattrix.api.responses.PollResponse;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollEventDto {
    private String type; // POLL_CREATED, POLL_VOTED, POLL_CLOSED, POLL_DELETED
    private PollResponse poll;
}
