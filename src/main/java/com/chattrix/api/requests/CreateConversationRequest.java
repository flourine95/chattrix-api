package com.chattrix.api.requests;

import com.chattrix.api.exceptions.BusinessException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class CreateConversationRequest {

    @NotBlank(message = "Conversation type is required")
    @Pattern(regexp = "DIRECT|GROUP", message = "Type must be either DIRECT or GROUP")
    private String type;

    private String name;

    @NotEmpty(message = "At least one participant is required")
    private List<Long> participantIds;

    public boolean isDirect() {
        return "DIRECT".equals(this.type);
    }

    public boolean isGroup() {
        return "GROUP".equals(this.type);
    }

    public Set<Long> getParticipantIdsExcluding(Long currentUserId) {
        return this.participantIds.stream()
                .filter(id -> id != null && !id.equals(currentUserId))
                .collect(Collectors.toSet());
    }

    public void validateParticipantCount(int otherCount) {
        if (isDirect() && otherCount != 1) {
            throw BusinessException.badRequest("DIRECT conversation must have exactly 1 other participant");
        }

        if (isGroup() && otherCount < 1) {
            throw BusinessException.badRequest("GROUP must have at least 2");
        }
    }
}
