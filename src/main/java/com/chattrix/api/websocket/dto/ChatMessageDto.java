package com.chattrix.api.websocket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatMessageDto {
    private Long conversationId;
    private String content;
    private String type;

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

    // Reply and mentions
    private Long replyToMessageId;
    private List<Long> mentions;
}
