package com.chattrix.api.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorRequest {
    private Long conversationId;
    private boolean typing; // true = start typing, false = stop typing
}
