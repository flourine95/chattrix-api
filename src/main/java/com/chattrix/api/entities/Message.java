package com.chattrix.api.entities;

import com.chattrix.api.enums.MessageType;
import com.chattrix.api.enums.ScheduledStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_conversation", columnList = "conversation_id"),
        @Index(name = "idx_messages_sender", columnList = "sender_id"),
        @Index(name = "idx_messages_created_at", columnList = "created_at"),
        // Composite indexes for common queries
        @Index(name = "idx_messages_conv_sent", columnList = "conversation_id, sent_at"),
        @Index(name = "idx_messages_conv_type", columnList = "conversation_id, type"),
        @Index(name = "idx_messages_sender_scheduled", columnList = "sender_id, scheduled_status, scheduled_time"),
        @Index(name = "idx_messages_conv_pinned", columnList = "conversation_id, pinned, pinned_at")
})
@SQLDelete(sql = "UPDATE messages SET deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted = false")
@NamedEntityGraph(
        name = "Message.fullContext",
        attributeNodes = {
                @NamedAttributeNode("sender"),
                @NamedAttributeNode(value = "replyToMessage", subgraph = "messageDetails"),
                @NamedAttributeNode(value = "originalMessage", subgraph = "messageDetails")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "messageDetails",
                        attributeNodes = @NamedAttributeNode("sender")
                )
        }
)
@NamedEntityGraph(
        name = "Message.withSenderAndConversation",
        attributeNodes = {
                @NamedAttributeNode("sender"),
                @NamedAttributeNode("conversation")
        }
)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MessageType type;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reactions", columnDefinition = "jsonb")
    private Map<String, List<Long>> reactions = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mentions", columnDefinition = "jsonb")
    private List<Long> mentions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    @NotFound(action = NotFoundAction.IGNORE)  // Ignore if replied message is deleted
    private Message replyToMessage;

    @Builder.Default
    @Column(name = "forwarded", nullable = false)
    private boolean forwarded = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_message_id")
    @NotFound(action = NotFoundAction.IGNORE)  // Ignore if original message is deleted
    private Message originalMessage;

    @Builder.Default
    @Column(name = "forward_count")
    private Integer forwardCount = 0;

    @Builder.Default
    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "pinned_at")
    private Instant pinnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_by")
    private User pinnedBy;

    @Builder.Default
    @Column(name = "scheduled", nullable = false)
    private boolean scheduled = false;

    @Column(name = "scheduled_time")
    private Instant scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "scheduled_status")
    private ScheduledStatus scheduledStatus;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "edited", nullable = false)
    private boolean edited = false;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onPrePersist() {
        this.createdAt = this.updatedAt = Instant.now();
        // Không set sentAt cho tin nhắn hẹn giờ
        if (!scheduled && this.sentAt == null) {
            this.sentAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    // ==================== HELPER METHODS ====================

    public boolean belongsToConversation(Long conversationId) {
        return this.conversation != null && this.conversation.getId().equals(conversationId);
    }

    public boolean isSentBy(Long userId) {
        return this.sender != null && this.sender.hasId(userId);
    }

    public boolean isOwnedBy(Long userId) {
        return isSentBy(userId);
    }

    public boolean isPollMessage() {
        return this.type == MessageType.POLL;
    }

    public boolean isEventMessage() {
        return this.type == MessageType.EVENT;
    }

    // ==================== METADATA STRUCTURE DOCUMENTATION ====================

    /*
     * JSONB metadata structure for different message types.
     *
     * IMPORTANT: The metadata field is a Map<String, Object> stored as JSONB in PostgreSQL.
     * The inner classes below are DOCUMENTATION ONLY - they describe the expected structure
     * but are NOT used in the actual code. All metadata manipulation is done directly on
     * the Map<String, Object> for simplicity and flexibility.
     *
     * Frontend should access metadata directly:
     * - message.metadata.poll.options[0].votes
     * - message.metadata.event.going
     * - message.metadata.mediaUrl
     * - message.metadata.latitude
     *
     * ==================== POLL METADATA ====================
     * Stored in: metadata.poll (Map<String, Object>)
     *
     * Structure:
     * {
     *   "poll": {
     *     "question": "What time works best?",
     *     "options": [
     *       {"id": 0, "text": "9 AM", "votes": [1, 2, 3]},
     *       {"id": 1, "text": "2 PM", "votes": [4, 5]}
     *     ],
     *     "allowMultiple": false,
     *     "anonymous": false,
     *     "closesAt": "2026-01-31T23:59:59Z"
     *   }
     * }
     *
     * Fields:
     * - question (String): The poll question
     * - options (List<Map>): Poll options with id, text, and votes
     *   - id (Long): Option index (0, 1, 2, ...)
     *   - text (String): Option text
     *   - votes (List<Long>): User IDs who voted for this option
     * - allowMultiple (Boolean): Allow multiple votes per user
     * - anonymous (Boolean): Hide voter identities
     * - closesAt (String): ISO-8601 timestamp when poll closes (optional, e.g., "2026-01-31T23:59:59Z")
     *
     * Calculated at runtime (NOT stored):
     * - totalVotes: Sum of all votes across options
     * - isClosed: Whether current time > closesAt
     *
     * ==================== EVENT METADATA ====================
     * Stored in: metadata.event (Map<String, Object>)
     *
     * Structure:
     * {
     *   "event": {
     *     "title": "Team Lunch",
     *     "description": "Monthly team lunch",
     *     "startTime": 1736942400.000000000,  // Instant (Jackson serializes)
     *     "endTime": 1736947800.000000000,    // Instant (Jackson serializes)
     *     "location": "Restaurant ABC",
     *     "going": [1, 2, 3],
     *     "maybe": [4, 5],
     *     "notGoing": [6]
     *   }
     * }
     *
     * Fields:
     * - title (String): Event title
     * - description (String): Event description
     * - startTime (String): ISO-8601 timestamp (e.g., "2026-01-15T12:00:00Z")
     * - endTime (String): ISO-8601 timestamp (e.g., "2026-01-15T13:30:00Z")
     * - location (String): Event location
     * - going (List<Long>): User IDs who are going
     * - maybe (List<Long>): User IDs who might go
     * - notGoing (List<Long>): User IDs who are not going
     *
     * Calculated at runtime (NOT stored):
     * - isPast: Whether current time > endTime
     * - goingCount, maybeCount, notGoingCount: List sizes
     *
     * ==================== MEDIA METADATA ====================
     * Stored in: metadata (Map<String, Object>)
     *
     * For IMAGE, VIDEO, AUDIO, FILE messages:
     * {
     *   "mediaUrl": "https://example.com/file.jpg",
     *   "thumbnailUrl": "https://example.com/thumb.jpg",
     *   "fileName": "vacation.jpg",
     *   "fileSize": 1024000,
     *   "duration": 120  // seconds, for video/audio only
     * }
     *
     * ==================== LOCATION METADATA ====================
     * Stored in: metadata (Map<String, Object>)
     *
     * For LOCATION messages:
     * {
     *   "latitude": 21.028511,
     *   "longitude": 105.804817,
     *   "locationName": "Hanoi, Vietnam"
     * }
     *
     * ==================== OTHER METADATA ====================
     *
     * Edit History:
     * {
     *   "editHistory": [
     *     {
     *       "oldContent": "Hello",
     *       "newContent": "Hello World",
     *       "editedAt": "2026-01-02T10:00:00Z",
     *       "editedBy": 1
     *     }
     *   ]
     * }
     *
     * Forward Info:
     * {
     *   "forwardedFrom": {
     *     "conversationId": 1,
     *     "messageId": 123,
     *     "originalSenderId": 5,
     *     "originalSenderUsername": "user5"
     *   }
     * }
     */
}