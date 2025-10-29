package com.chattrix.api.websocket.dto;

import com.chattrix.api.responses.MentionedUserResponse;
import com.chattrix.api.responses.ReplyMessageResponse;
import com.chattrix.api.responses.UserResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO for outgoing messages broadcast to clients via WebSocket
 * Contains additional information like sender details
 */
@Getter
@Setter
public class OutgoingMessageDto {
    private Long id;
    private Long conversationId;
    private UserResponse sender;
    private String content;
    private String type;
    private Instant createdAt;

    // Rich media fields
    private String mediaUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private Integer duration;

    // Location fields
    private Double latitude;
    private Double longitude;
    private String locationName;

    // Reply context
    private Long replyToMessageId;
    private ReplyMessageResponse replyToMessage;

    // Reactions
    private Map<String, List<Long>> reactions;

    // Mentions
    private List<Long> mentions;
    private List<MentionedUserResponse> mentionedUsers;
}

