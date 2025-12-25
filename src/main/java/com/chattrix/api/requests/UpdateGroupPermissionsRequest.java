package com.chattrix.api.requests;

import lombok.Data;

@Data
public class UpdateGroupPermissionsRequest {
    private String sendMessages;      // ALL, ADMIN_ONLY
    private String addMembers;         // ALL, ADMIN_ONLY
    private String removeMembers;      // ADMIN_ONLY
    private String editGroupInfo;      // ALL, ADMIN_ONLY
    private String pinMessages;        // ALL, ADMIN_ONLY
    private String deleteMessages;     // OWNER, ADMIN_ONLY, ALL
    private String createPolls;        // ALL, ADMIN_ONLY
}
