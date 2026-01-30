package com.chattrix.api.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotePollRequest {
    @NotNull(message = "Option IDs are required")
    // Removed @NotEmpty to allow unvoting (empty array = remove all votes)
    private List<Long> optionIds;
}
