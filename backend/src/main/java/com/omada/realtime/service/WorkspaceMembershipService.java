package com.omada.realtime.service;

import com.omada.realtime.entity.WorkspaceMembership;

import java.util.List;

/**
 * Workspace membership management. Roles: OWNER > ADMIN > MEMBER > VIEWER.
 */
public interface WorkspaceMembershipService {

    /** Adds a user to a workspace with the given role. */
    WorkspaceMembership create(String workspaceId, String userId, String role);

    /** Lists all members of a workspace. */
    List<WorkspaceMembership> findByWorkspace(String workspaceId);

    /** Changes a member's role within a workspace. */
    WorkspaceMembership changeRole(String workspaceId, String userId, String role);

    /** Removes a member from a workspace. */
    void remove(String workspaceId, String userId);
}
