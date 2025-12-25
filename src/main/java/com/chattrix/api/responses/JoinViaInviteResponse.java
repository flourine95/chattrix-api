package com.chattrix.api.responses;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinViaInviteResponse {
    private Boolean success;
    private Long conversationId;
    private String message;
}
