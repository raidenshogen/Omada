package com.omada.realtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A user assigned to a specific workspace. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberResponse {
    private String id;
    private String workspaceId;
    private String userId;
    private String role;
    private String email;
    private String name;
    private String joinedAt;
}
