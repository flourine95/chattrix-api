package com.chattrix.api.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdayUserResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private Instant dateOfBirth;
    private Integer age;
    private String birthdayMessage; // "Hôm nay", "Còn 3 ngày", etc.
}
