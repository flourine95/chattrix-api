package com.chattrix.api.dto;

import lombok.*;

import java.util.List;

/**
 * DTO wrapper cho Message metadata JSONB column.
 * Dùng để type-safe access thay vì Map.put()
 * 
 * KHÔNG dùng manual conversion - dùng MapStruct mapper!
 * 
 * Database structure (JSONB) - xem DATABASE-JSONB-STRUCTURE.md
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageMetadata {
    
    // ==================== Media Fields ====================
    private String mediaUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private Integer duration; // seconds
    
    // ==================== Location Fields ====================
    private Double latitude;
    private Double longitude;
    private String locationName;
    
    // ==================== System Message Fields ====================
    private Long kickedBy;
    private Long addedBy;
    private List<Long> addedUserIds;
    private Long promotedBy;
    private Long demotedBy;
    private Long mutedBy;
    private Long unmutedBy;
    private Long invitedBy;
    private String oldName;
    private String newName;
    private Long mutedUntil; // epoch millis
    private String failedReason;
    
    // ==================== Nested Objects (Poll, Event) ====================
    // Để Map vì structure phức tạp, sẽ có DTO riêng
    private Object poll;
    private Object event;
    
    // ==================== Convenience Methods ====================
    
    public boolean hasMedia() {
        return mediaUrl != null;
    }
    
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }
    
    public boolean hasPoll() {
        return poll != null;
    }
    
    public boolean hasEvent() {
        return event != null;
    }
}
