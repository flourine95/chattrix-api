package com.chattrix.api.websocket.dto;

import com.chattrix.api.responses.ReplyMessageResponse;
import com.chattrix.api.responses.UserResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class OutgoingMessageDto {
    private Long id;
    private Long conversationId;
    private UserResponse sender;
    private String content;
    private String type;
    private Instant createdAt;

    // Metadata (contains poll, event, and other data)
    private Map<String, Object> metadata;

    // Rich media fields (deprecated - use metadata instead)
    private String mediaUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private Integer duration;

    // Location fields (deprecated - use metadata instead)
    private Double latitude;
    private Double longitude;
    private String locationName;

    // Reply context
    private Long replyToMessageId;
    private ReplyMessageResponse replyToMessage;

    // Reactions
    private Map<String, List<Long>> reactions;

    // Mentions: [1, 2, 3] - user IDs who were mentioned
    private List<Long> mentions;
}

