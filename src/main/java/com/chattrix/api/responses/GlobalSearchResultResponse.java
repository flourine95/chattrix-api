package com.chattrix.api.responses;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchResultResponse {
    // Message info
    private Long messageId;
    private String content;
    private String type;
    private Instant sentAt;
    
    // Sender info
    private Long senderId;
    private String senderUsername;
    private String senderFullName;
    private String senderAvatarUrl;
    
    // Conversation info
    private Long conversationId;
    private String conversationName;
    private String conversationType;
    private String conversationAvatarUrl;
}
