package com.omada.realtime.controller;

import com.omada.realtime.dto.request.CreateInvitationRequest;
import com.omada.realtime.entity.Invitation;
import com.omada.realtime.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Workspace invitations.
 * <ul>
 *   <li>{@code POST/GET /api/workspaces/{workspaceId}/invitations} — create/list (OWNER or ADMIN of the workspace).</li>
 *   <li>{@code GET/POST/POST /api/invitations/{token}} (+ /accept, /decline) — any holder of a valid token.</li>
 *   <li>{@code DELETE /api/invitations/{id}} — revoke (OWNER or ADMIN of the invitation's workspace; {@code id} is the invitation token).</li>
 * </ul>
 */
@RestController
public class InvitationController {

    private final InvitationService service;

    public InvitationController(InvitationService service) {
        this.service = service;
    }

    /** Create a PENDING invitation (workspace from path; email/role/optional boardId in body). */
    @PostMapping("/api/workspaces/{workspaceId}/invitations")
    @PreAuthorize("@workspaceSecurity.canWriteWorkspace(#workspaceId, authentication.name)")
    public Invitation create(@PathVariable String workspaceId,
                             @Valid @RequestBody CreateInvitationRequest req) {
        return service.create(workspaceId, req.getEmail(), req.getRole(), req.getBoardId());
    }

    /** List all invitations of a workspace. */
    @GetMapping("/api/workspaces/{workspaceId}/invitations")
    @PreAuthorize("@workspaceSecurity.canWriteWorkspace(#workspaceId, authentication.name)")
    public List<Invitation> list(@PathVariable String workspaceId) {
        return service.listByWorkspace(workspaceId).stream()
                .filter(inv -> "PENDING".equals(inv.getStatus()))
                .toList();
    }

    /** Invitation details by token — no role gate; the unguessable token is the grant. */
    @GetMapping("/api/invitations/{token}")
    @PreAuthorize("permitAll")
    public Invitation getByToken(@PathVariable UUID token) {
        return service.getByToken(token);
    }

    /** Accept the invitation identified by token (idempotent in the service). */
    @PostMapping("/api/invitations/{token}/accept")
    @PreAuthorize("permitAll")
    public Invitation accept(@PathVariable UUID token) {
        return service.accept(token);
    }

    /** Decline the invitation identified by token. */
    @PostMapping("/api/invitations/{token}/decline")
    @PreAuthorize("permitAll")
    public Invitation decline(@PathVariable UUID token) {
        return service.decline(token);
    }

    /** Revoke (delete) the invitation identified by {@code id} (the invitation token). */
    @DeleteMapping("/api/invitations/{id}")
    @PreAuthorize("@workspaceSecurity.isInvitationWorkspaceAdminOrOwner(#id, authentication.name)")
    public void revoke(@PathVariable("id") UUID id) {
        service.revoke(id);
    }
}
