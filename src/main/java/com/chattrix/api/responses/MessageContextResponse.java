package com.chattrix.api.responses;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageContextResponse {
    private Long targetMessageId;
    private List<MessageResponse> messages; // Messages around the target
    private Integer targetIndex; // Index of target message in the list
}
