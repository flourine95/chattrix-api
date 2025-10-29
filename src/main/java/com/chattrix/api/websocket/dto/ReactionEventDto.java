package com.chattrix.api.websocket.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ReactionEventDto {
    private Long messageId;
    private Long userId;
    private String userName;
    private String emoji;
    private String action; // "add" or "remove"
    private Map<String, List<Long>> reactions; // Updated reactions map
    private Instant timestamp;
}

