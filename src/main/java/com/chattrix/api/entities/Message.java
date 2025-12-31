package com.chattrix.api.entities;

import com.chattrix.api.enums.MessageType;
import com.chattrix.api.enums.ScheduledStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
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
        @Index(name = "idx_messages_created_at", columnList = "created_at")
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
    private Message replyToMessage;

    @Builder.Default
    @Column(name = "forwarded", nullable = false)
    private boolean forwarded = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_message_id")
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
}