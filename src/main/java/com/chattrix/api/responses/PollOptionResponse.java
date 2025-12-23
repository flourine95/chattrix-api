package com.chattrix.api.responses;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionResponse {
    private Long id;
    private String optionText;
    private Integer optionOrder;
    private Integer voteCount;
    private Double percentage;
    private List<UserResponse> voters;
}
