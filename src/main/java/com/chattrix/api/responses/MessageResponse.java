package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderUsername;
    private String senderName;
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

    // Reply context
    private Long replyToMessageId;
    private ReplyMessageResponse replyToMessage;

    // Reactions: {"👍": [1, 2, 3], "❤️": [4, 5]}
    private Map<String, List<Long>> reactions;

    // Mentions
    private List<Long> mentions;
    private List<MentionedUserResponse> mentionedUsers;

    private Instant sentAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Edit/Delete/Forward
    private boolean isEdited;
    private Instant editedAt;
    private boolean isDeleted;
    private Instant deletedAt;
    private boolean isForwarded;
    private Long originalMessageId;
    private Integer forwardCount;

    // Read receipts
    private Long readCount;
    private List<ReadReceiptResponse> readBy;
}
