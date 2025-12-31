package com.chattrix.api.dto.metadata;

import lombok.*;

import java.util.List;

/**
 * DTO for system message metadata
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemMessageMetadata {
    // User actions
    private Long kickedBy;
    private Long addedBy;
    private List<Long> addedUserIds;
    private Long promotedBy;
    private Long demotedBy;
    private Long mutedBy;
    private Long unmutedBy;
    private Long invitedBy;
    
    // Group changes
    private String oldName;
    private String newName;
    
    // Mute info
    private Long mutedUntil; // Epoch millis
    
    // Failure info
    private String failedReason;
}
