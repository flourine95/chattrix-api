package com.chattrix.api.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class MediaStatisticsResponse {
    private Long totalImages;
    private Long totalVideos;
    private Long totalAudios;
    private Long totalFiles;
    private Long totalLinks;
    private Long totalMedia; // Sum of all media types
    private Long totalSize;  // Total file size in bytes (if available in metadata)
}
