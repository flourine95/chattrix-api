package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ReactionResponse {
    private Long messageId;
    private Map<String, List<Long>> reactions;
}

