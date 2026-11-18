package com.omada.realtime.controller;

import com.omada.realtime.dto.response.WorkspaceMemberResponse;
import com.omada.realtime.entity.User;
import com.omada.realtime.entity.WorkspaceMembership;
import com.omada.realtime.repository.UserRepository;
import com.omada.realtime.service.WorkspaceMembershipService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/members")
public class WorkspaceMembershipController {
    private final WorkspaceMembershipService service;
    private final UserRepository users;

    public WorkspaceMembershipController(WorkspaceMembershipService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.canViewWorkspace(#workspaceId, authentication.name)")
    public List<WorkspaceMemberResponse> list(@PathVariable String workspaceId) {
        return service.findByWorkspace(workspaceId).stream().map(this::toResponse).toList();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@workspaceSecurity.canManageWorkspaceMember(#workspaceId, #userId, authentication.name)")
    public void remove(@PathVariable String workspaceId, @PathVariable String userId) {
        service.remove(workspaceId, userId);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("@workspaceSecurity.canManageWorkspaceMember(#workspaceId, #userId, authentication.name)")
    public WorkspaceMemberResponse changeRole(@PathVariable String workspaceId,
                                              @PathVariable String userId,
                                              @Valid @RequestBody com.omada.realtime.dto.request.UpdateRoleRequest request) {
        return toResponse(service.changeRole(workspaceId, userId, request.getRole()));
    }

    private WorkspaceMemberResponse toResponse(WorkspaceMembership membership) {
        User user = users.findById(java.util.UUID.fromString(membership.getUserId())).orElse(null);
        return new WorkspaceMemberResponse(
                membership.getId().toString(),
                membership.getWorkspaceId(),
                membership.getUserId(),
                membership.getRole().toLowerCase(),
                user != null ? user.getEmail() : null,
                user != null ? user.getName() : null,
                membership.getJoinedAt().toString());
    }
}
