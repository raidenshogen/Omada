package com.omada.realtime.security;

import com.omada.realtime.dto.response.WorkspaceResponse;
import com.omada.realtime.entity.Board;
import com.omada.realtime.entity.Card;
import com.omada.realtime.repository.BoardMembershipRepository;
import com.omada.realtime.entity.Invitation;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.CardRepository;
import com.omada.realtime.repository.InvitationRepository;
import com.omada.realtime.repository.WorkspaceMembershipRepository;
import com.omada.realtime.service.MembershipService;
import com.omada.realtime.service.WorkspaceService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * RBAC checks used by @PreAuthorize expressions. The hierarchy is
 * OWNER > ADMIN > MEMBER > VIEWER, resolved from membership records.
 * Checks return false (never throw) so Spring Security answers 403.
 *
 * Role model:
 *  - read anything in the workspace  = any membership (VIEWER+)
 *  - edit content (lists/cards/... ) = MEMBER+
 *  - change board structure          = ADMIN+
 *  - change org members / delete org = OWNER
 *
 * Cards and boards carry only their parent id (boardId / workspaceId), so the
 * checks walk up to the owning workspace and resolve its organization's
 * membership. Unknown or malformed ids resolve to null and are denied.
 */
@Component("workspaceSecurity")
public class WorkspaceSecurity {

    private final MembershipService memberships;
    private final WorkspaceService workspaces;
    private final BoardRepository boards;
    private final CardRepository cards;
    private final BoardMembershipRepository boardMemberships;
    private final InvitationRepository invitations;
    private final ObjectProvider<WorkspaceMembershipRepository> workspaceMemberships;
    private final StringRedisTemplate redis;

    private static final Duration WORKSPACE_ORG_TTL = Duration.ofMinutes(10);

    public WorkspaceSecurity(MembershipService memberships,
                             WorkspaceService workspaces,
                             BoardRepository boards,
                             CardRepository cards,
                             BoardMembershipRepository boardMemberships,
                             InvitationRepository invitations,
                             ObjectProvider<WorkspaceMembershipRepository> workspaceMemberships,
                             StringRedisTemplate redis) {
        this.memberships = memberships;
        this.workspaces = workspaces;
        this.boards = boards;
        this.cards = cards;
        this.boardMemberships = boardMemberships;
        this.invitations = invitations;
        this.workspaceMemberships = workspaceMemberships;
        this.redis = redis;
    }

    /** Any membership role (even VIEWER) may read organization data. */
    public boolean isOrgMember(String organizationId, String userId) {
        return memberships.hasRole(organizationId, userId, "VIEWER");
    }

    /** MEMBER or above: may create/edit workspace content. */
    public boolean isOrgWriter(String organizationId, String userId) {
        return memberships.hasRole(organizationId, userId, "MEMBER");
    }

    /** ADMIN or OWNER: can add/remove members and change workspace structure. */
    public boolean isOrgAdmin(String organizationId, String userId) {
        return memberships.hasRole(organizationId, userId, "ADMIN");
    }

    /** OWNER only: can change roles or delete the organization. */
    public boolean isOrgOwner(String organizationId, String userId) {
        return memberships.hasRole(organizationId, userId, "OWNER");
    }

    /** Any member of the board's workspace may list its members. */
    public boolean isBoardMember(String boardId, String userId) {
        String wsId = boardWorkspace(boardId);
        return wsId != null && canViewWorkspace(wsId, userId);
    }

    /** ADMIN or OWNER of the board's workspace organization may change board membership. */
    public boolean isBoardAdmin(String boardId, String userId) {
        String wsId = boardWorkspace(boardId);
        String orgId = wsId != null ? workspaceOrg(wsId) : null;
        return orgId != null && isOrgAdmin(orgId, userId);
    }

    /** Read access to a workspace = membership in its organization. */
    public boolean canViewWorkspace(String workspaceId, String userId) {
        String orgId = workspaceOrg(workspaceId);
        WorkspaceMembershipRepository repository = workspaceMemberships.getIfAvailable();
        return (orgId != null && isOrgMember(orgId, userId))
                || (repository != null && repository.existsByWorkspaceIdAndUserId(workspaceId, userId));
    }

    /** Allows an invited user to list only workspaces they directly belong to. */
    public boolean canListUserWorkspaces(String userId) {
        WorkspaceMembershipRepository repository = workspaceMemberships.getIfAvailable();
        return userId != null;
    }

    /** Write access to a workspace itself = ADMIN or OWNER. */
    public boolean canWriteWorkspace(String workspaceId, String userId) {
        String orgId = workspaceOrg(workspaceId);
        if (orgId != null && isOrgAdmin(orgId, userId)) {
            return true;
        }
        WorkspaceMembershipRepository repository = workspaceMemberships.getIfAvailable();
        if (repository == null) {
            return false;
        }
        return repository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(membership -> "ADMIN".equalsIgnoreCase(membership.getRole())
                        || "OWNER".equalsIgnoreCase(membership.getRole()))
                .orElse(false);
    }

    /** Allows an owner/admin to remove a lower-ranked member from this workspace. */
    public boolean canManageWorkspaceMember(String workspaceId, String targetUserId, String actorUserId) {
        if (targetUserId == null || actorUserId == null || targetUserId.equals(actorUserId)) {
            return false;
        }
        WorkspaceMembershipRepository repository = workspaceMemberships.getIfAvailable();
        if (repository == null) {
            return false;
        }
        var target = repository.findByWorkspaceIdAndUserId(workspaceId, targetUserId);
        if (target.isEmpty()) {
            return false;
        }
        String actorRole = repository.findByWorkspaceIdAndUserId(workspaceId, actorUserId)
                .map(membership -> membership.getRole().toUpperCase())
                .orElseGet(() -> {
                    String orgId = workspaceOrg(workspaceId);
                    if (orgId != null && isOrgOwner(orgId, actorUserId)) return "OWNER";
                    if (orgId != null && isOrgAdmin(orgId, actorUserId)) return "ADMIN";
                    // Fallback: check if actor is a member of the workspace with any role
                    return repository.findByWorkspaceIdAndUserId(workspaceId, actorUserId)
                            .map(m -> m.getRole().toUpperCase())
                            .orElseGet(() -> "");
                });
        return roleRank(actorRole) >= 3
                && roleRank(actorRole) > roleRank(target.get().getRole());
    }

    private int roleRank(String role) {
        return switch (role == null ? "" : role.toUpperCase()) {
            case "OWNER" -> 4;
            case "ADMIN" -> 3;
            case "MEMBER" -> 2;
            case "VIEWER" -> 1;
            default -> 0;
        };
    }

    /** Write access to a workspace's content (documents, lists, cards, comments) = MEMBER+. */
    public boolean canWriteWorkspaceContent(String workspaceId, String userId) {
        return roleRank(effectiveWorkspaceRole(workspaceId, userId)) >= 2;
    }

    /** Ownership of a workspace's organization = OWNER role. */
    public boolean isWorkspaceOwner(String workspaceId, String userId) {
        String orgId = workspaceOrg(workspaceId);
        return orgId != null && isOrgOwner(orgId, userId);
    }

    /** Legacy alias used by board endpoints: read a workspace's boards. */
    public boolean canView(String workspaceId, String userId) {
        return canViewWorkspace(workspaceId, userId);
    }

    /** Legacy alias used by board endpoints: create/update a board (ADMIN+). */
    public boolean hasBoardWrite(String workspaceId, String userId) {
        return canWriteWorkspace(workspaceId, userId);
    }

    /** Legacy alias used by board endpoints: delete a board (OWNER). */
    public boolean hasWorkspaceAdmin(String workspaceId, String userId) {
        return isWorkspaceOwner(workspaceId, userId);
    }

    /** Read a board: any member of the board's workspace organization. */
    public boolean canViewBoard(String boardId, String userId) {
        String wsId = boardWorkspace(boardId);
        return wsId != null && canViewWorkspace(wsId, userId);
    }

    /** Edit a board's content (lists, cards, comments): MEMBER+. */
    public boolean canEditBoard(String boardId, String userId) {
        String workspaceId = boardWorkspace(boardId);
        return workspaceId != null && canWriteWorkspaceContent(workspaceId, userId);
    }

    /** Read a card: any member of the card's board's workspace organization. */
    public boolean canViewCard(String cardId, String userId) {
        String wsId = cardWorkspace(cardId);
        return wsId != null && canViewWorkspace(wsId, userId);
    }

    /** Edit a card or its comments: MEMBER+. */
    public boolean canEditCard(String cardId, String userId) {
        String workspaceId = cardWorkspace(cardId);
        return workspaceId != null && canWriteWorkspaceContent(workspaceId, userId);
    }

    /**
     * Direct workspace memberships take precedence over organization membership.
     * This is required for invited users, who may not have an organization role.
     */
    private String effectiveWorkspaceRole(String workspaceId, String userId) {
        WorkspaceMembershipRepository repository = workspaceMemberships.getIfAvailable();
        if (repository != null) {
            var directMembership = repository.findByWorkspaceIdAndUserId(workspaceId, userId);
            if (directMembership.isPresent()) {
                return directMembership.get().getRole();
            }
        }
        String orgId = workspaceOrg(workspaceId);
        if (orgId != null) {
            if (isOrgOwner(orgId, userId)) return "OWNER";
            if (isOrgAdmin(orgId, userId)) return "ADMIN";
            if (isOrgWriter(orgId, userId)) return "MEMBER";
            if (isOrgMember(orgId, userId)) return "VIEWER";
        }
        return "";
    }

    /** Owner or ADMIN of the workspace that owns the invitation identified by token. */
    public boolean isInvitationWorkspaceAdminOrOwner(UUID token, String userId) {
        try {
            Invitation inv = invitations.findByToken(token).orElse(null);
            if (inv == null) {
                return false;
            }
            return canWriteWorkspace(inv.getWorkspaceId(), userId);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Resolves which organization a workspace belongs to (null if unknown). */
    private String workspaceOrg(String workspaceId) {
        String cacheKey = "workspace_org:" + workspaceId;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }
        try {
            WorkspaceResponse ws = workspaces.findById(workspaceId);
            String orgId = ws != null ? ws.getOrganizationId() : null;
            // Cache negative results (empty string) briefly to avoid repeated DB hits for unknown IDs
            redis.opsForValue().set(cacheKey, orgId == null ? "" : orgId, WORKSPACE_ORG_TTL);
            return orgId;
        } catch (Exception e) {
            return null; // unknown workspace -> deny
        }
    }

    /** Resolves the workspace id a board belongs to (null if unknown). */
    private String boardWorkspace(String boardId) {
        try {
            return boards.findById(UUID.fromString(boardId))
                    .map(Board::getWorkspaceId)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** Resolves the organization a board belongs to (null if unknown). */
    private String boardOrg(String boardId) {
        String wsId = boardWorkspace(boardId);
        return wsId != null ? workspaceOrg(wsId) : null;
    }

    /** Resolves the workspace id a card belongs to (null if unknown). */
    private String cardWorkspace(String cardId) {
        try {
            return cards.findById(UUID.fromString(cardId))
                    .map(Card::getBoardId)
                    .map(this::boardWorkspace)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** Resolves the organization a card belongs to (null if unknown). */
    private String cardOrg(String cardId) {
        String wsId = cardWorkspace(cardId);
        return wsId != null ? workspaceOrg(wsId) : null;
    }
}
