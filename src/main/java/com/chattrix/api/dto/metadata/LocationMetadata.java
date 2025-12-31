package com.chattrix.api.dto.metadata;

import lombok.*;

/**
 * DTO for location-related metadata
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationMetadata {
    private Double latitude;
    private Double longitude;
    private String locationName;
}
