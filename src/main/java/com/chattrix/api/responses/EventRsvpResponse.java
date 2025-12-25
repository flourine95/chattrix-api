package com.chattrix.api.responses;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRsvpResponse {

    private Long id;
    private UserResponse user;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}

