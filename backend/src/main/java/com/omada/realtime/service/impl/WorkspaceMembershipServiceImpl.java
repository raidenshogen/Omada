package com.omada.realtime.service.impl;

import com.omada.realtime.entity.WorkspaceMembership;
import com.omada.realtime.repository.WorkspaceMembershipRepository;
import com.omada.realtime.security.IdGenerator;
import com.omada.realtime.service.WorkspaceMembershipService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Workspace membership business logic with the role hierarchy
 * OWNER > ADMIN > MEMBER > VIEWER.
 * - adding a member that already exists is rejected
 * - role changes are validated server-side against the allowed role set
 */
@Service
public class WorkspaceMembershipServiceImpl implements WorkspaceMembershipService {

    private static final Set<String> VALID_ROLES = Set.of("OWNER", "ADMIN", "MEMBER", "VIEWER");

    private final WorkspaceMembershipRepository memberships;

    public WorkspaceMembershipServiceImpl(WorkspaceMembershipRepository memberships) {
        this.memberships = memberships;
    }

    @Override
    public WorkspaceMembership create(String workspaceId, String userId, String role) {
        String normalized = normalizeRole(role);
        if (memberships.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new IllegalArgumentException("User is already a member of this workspace");
        }
        WorkspaceMembership m = WorkspaceMembership.builder()
                .id(IdGenerator.uuid())
                .workspaceId(workspaceId)
                .userId(userId)
                .role(normalized)
                .joinedAt(Instant.now())
                .build();
        return memberships.save(m);
    }

    @Override
    public List<WorkspaceMembership> findByWorkspace(String workspaceId) {
        return memberships.findByWorkspaceId(workspaceId);
    }

    @Override
    public WorkspaceMembership changeRole(String workspaceId, String userId, String role) {
        String normalized = normalizeRole(role);
        if ("OWNER".equals(normalized)) {
            throw new IllegalArgumentException("Owner role cannot be assigned through workspace membership");
        }
        WorkspaceMembership m = memberships.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Not a member of this workspace"));
        m.setRole(normalized);
        return memberships.save(m);
    }

    @Override
    public void remove(String workspaceId, String userId) {
        WorkspaceMembership m = memberships.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Not a member of this workspace"));
        memberships.delete(m);
    }

    /** Role must be one of OWNER, ADMIN, MEMBER, VIEWER (case-insensitive). */
    private String normalizeRole(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        String upper = role.trim().toUpperCase();
        if (!VALID_ROLES.contains(upper)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        return upper;
    }
}
