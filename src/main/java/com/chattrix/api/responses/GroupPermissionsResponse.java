package com.chattrix.api.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPermissionsResponse {
    private Long conversationId;
    private String sendMessages;
    private String addMembers;
    private String removeMembers;
    private String editGroupInfo;
    private String pinMessages;
    private String deleteMessages;
    private String createPolls;
}
