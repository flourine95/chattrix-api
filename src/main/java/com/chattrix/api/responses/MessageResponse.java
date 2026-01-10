package com.chattrix.api.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderUsername;
    private String senderFullName;
    private String senderAvatarUrl;
    private String content;
    private String type;

    // Metadata as object (contains: mediaUrl, fileName, fileSize, duration, latitude, longitude, locationName, editHistory, failedReason, poll, event, etc.)
    private Map<String, Object> metadata;

    // Reply context
    private Long replyToMessageId;
    private ReplyMessageResponse replyToMessage;

    // Reactions: {"👍": [1, 2, 3], "❤️": [4, 5]}
    private Map<String, List<Long>> reactions;

    // Mentions: [1, 2, 3] - user IDs who were mentioned
    private List<Long> mentions;

    private Instant sentAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Edit/Delete/Forward
    private boolean edited;
    private Instant editedAt;
    private boolean deleted;
    private Instant deletedAt;
    private boolean forwarded;
    private Long originalMessageId;
    private Integer forwardCount;

    // Pinned message fields
    private Boolean pinned;
    private Instant pinnedAt;
    private Long pinnedBy;
    private String pinnedByUsername;
    private String pinnedByFullName;

    private Long readCount;

    private List<ReadReceiptResponse> readBy;

    // Scheduled message fields
    private Boolean scheduled;
    private Instant scheduledTime;
    private String scheduledStatus;
}
