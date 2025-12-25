package com.chattrix.api.requests;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventRequest {

    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    private String description;

    private Instant startTime;

    private Instant endTime;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;
}

