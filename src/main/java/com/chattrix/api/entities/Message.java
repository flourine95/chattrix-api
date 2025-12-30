package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "messages")
@SQLDelete(sql = "UPDATE messages SET deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted = false")
@NamedEntityGraph(
        name = "Message.withSenderAndReply",
        attributeNodes = {
                @NamedAttributeNode("sender"),
                @NamedAttributeNode(value = "replyToMessage", subgraph = "replySubgraph")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "replySubgraph",
                        attributeNodes = @NamedAttributeNode("sender")
                )
        }
)
@NamedEntityGraph(
        name = "Message.withSenderAndConversation",
        attributeNodes = {
                @NamedAttributeNode("sender"),
                @NamedAttributeNode("conversation"),
                @NamedAttributeNode(value = "replyToMessage", subgraph = "replySubgraph")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "replySubgraph",
                        attributeNodes = @NamedAttributeNode("sender")
                )
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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    // Rich media fields
    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "duration")
    private Integer duration;

    // Location fields
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "location_name")
    private String locationName;

    // Reply to message
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;

    // Reply to note (for Messenger Notes feature)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_note_id")
    private UserNote replyToNote;

    // Poll reference (for POLL message type)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id")
    private Poll poll;

    // Event reference (for EVENT message type)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    // Reactions stored as JSONB: {"👍": [1, 2, 3], "❤️": [4, 5]}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reactions", columnDefinition = "jsonb")
    private Map<String, List<Long>> reactions = new HashMap<>();

    // Mentions stored as JSONB array: [123, 456, 789]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mentions", columnDefinition = "jsonb")
    private List<Long> mentions;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Message editing
    @Column(name = "edited", nullable = false)
    private boolean edited = false;

    @Column(name = "edited_at")
    private Instant editedAt;

    // Message deletion (soft delete)
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    // Message forwarding
    @Column(name = "forwarded", nullable = false)
    private boolean forwarded = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_message_id")
    private Message originalMessage;

    @Column(name = "forward_count")
    private Integer forwardCount = 0;

    // Pinned message fields
    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "pinned_at")
    private Instant pinnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_by")
    private User pinnedBy;

    // Scheduled message fields
    @Column(name = "scheduled", nullable = false)
    private boolean scheduled = false;

    @Column(name = "scheduled_time")
    private Instant scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "scheduled_status", length = 20)
    private ScheduledStatus scheduledStatus;

    @Column(name = "failed_reason", columnDefinition = "TEXT")
    private String failedReason;

    @PrePersist
    protected void onPrePersist() {
        // Only set sentAt for non-scheduled messages
        if (!scheduled) {
            this.sentAt = Instant.now();
        }
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum MessageType {
        TEXT,
        IMAGE,
        LINK,
        VIDEO,
        VOICE,
        AUDIO,
        DOCUMENT,
        LOCATION,
        STICKER,
        EMOJI,
        SYSTEM,
        POLL,
        EVENT,
        ANNOUNCEMENT  // Admin-only important messages
    }

    public enum ScheduledStatus {
        PENDING,    // Chờ gửi
        SENT,       // Đã gửi thành công
        FAILED,     // Gửi thất bại
        CANCELLED   // Đã hủy
    }
}
