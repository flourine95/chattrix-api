package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class MediaResponse {
    private Long id;
    private String type; // IMAGE, VIDEO, AUDIO, DOCUMENT
    private String mediaUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private Integer duration; // For video/audio
    private Long senderId;
    private String senderUsername;
    private Instant sentAt;
}

