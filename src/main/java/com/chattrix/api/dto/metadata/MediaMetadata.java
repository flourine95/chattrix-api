package com.chattrix.api.dto.metadata;

import lombok.*;

/**
 * DTO for media-related metadata (images, videos, audio, files)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaMetadata {
    private String mediaUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private Integer duration; // For audio/video in seconds
}
