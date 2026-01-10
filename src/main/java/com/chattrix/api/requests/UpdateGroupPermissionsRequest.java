package com.chattrix.api.requests;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateGroupPermissionsRequest {
    @Pattern(regexp = "ALL|ADMIN_ONLY", message = "sendMessages must be ALL or ADMIN_ONLY")
    private String sendMessages;      // ALL, ADMIN_ONLY
    
    @Pattern(regexp = "ALL|ADMIN_ONLY", message = "addMembers must be ALL or ADMIN_ONLY")
    private String addMembers;         // ALL, ADMIN_ONLY
    
    @Pattern(regexp = "ADMIN_ONLY", message = "removeMembers must be ADMIN_ONLY")
    private String removeMembers;      // ADMIN_ONLY
    
    @Pattern(regexp = "ALL|ADMIN_ONLY", message = "editGroupInfo must be ALL or ADMIN_ONLY")
    private String editGroupInfo;      // ALL, ADMIN_ONLY
    
    @Pattern(regexp = "ALL|ADMIN_ONLY", message = "pinMessages must be ALL or ADMIN_ONLY")
    private String pinMessages;        // ALL, ADMIN_ONLY
    
    @Pattern(regexp = "OWNER|ADMIN_ONLY|ALL", message = "deleteMessages must be OWNER, ADMIN_ONLY, or ALL")
    private String deleteMessages;     // OWNER, ADMIN_ONLY, ALL
    
    @Pattern(regexp = "ALL|ADMIN_ONLY", message = "createPolls must be ALL or ADMIN_ONLY")
    private String createPolls;        // ALL, ADMIN_ONLY
}
