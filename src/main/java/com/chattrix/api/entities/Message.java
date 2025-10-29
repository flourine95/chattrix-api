package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
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

    @PrePersist
    protected void onPrePersist() {
        this.sentAt = Instant.now();
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
        VIDEO,
        VOICE,
        AUDIO,
        DOCUMENT,
        LOCATION,
        SYSTEM
    }
}
