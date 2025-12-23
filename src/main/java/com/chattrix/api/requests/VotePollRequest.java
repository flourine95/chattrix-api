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
    @NotEmpty(message = "At least one option must be selected")
    private List<Long> optionIds;
}
