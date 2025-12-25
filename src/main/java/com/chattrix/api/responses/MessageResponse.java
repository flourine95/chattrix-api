package com.chattrix.api.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String senderFullName;
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

    // Poll reference (for POLL message type)
    private Long pollId;
    private PollResponse poll;

    // Event reference (for EVENT message type)
    private Long eventId;
    private EventResponse event;

    // Reactions: {"👍": [1, 2, 3], "❤️": [4, 5]}
    private Map<String, List<Long>> reactions;

    // Mentions
    private List<Long> mentions;
    private List<MentionedUserResponse> mentionedUsers;

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

    // Read receipts
    @JsonProperty("readCount")
    private Long readCount;

    @JsonProperty("readBy")
    private List<ReadReceiptResponse> readBy;

    // Scheduled message fields
    private Boolean scheduled;
    private Instant scheduledTime;
    private String scheduledStatus;
    private String failedReason;
}
