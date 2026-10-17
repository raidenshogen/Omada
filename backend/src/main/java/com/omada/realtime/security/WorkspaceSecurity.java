package com.omada.realtime.security;

import com.omada.realtime.dto.response.WorkspaceResponse;
import com.omada.realtime.entity.Board;
import com.omada.realtime.entity.Card;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.CardRepository;
import com.omada.realtime.service.MembershipService;
import com.omada.realtime.service.WorkspaceService;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redis;

    private static final Duration WORKSPACE_ORG_TTL = Duration.ofMinutes(10);

    public WorkspaceSecurity(MembershipService memberships,
                             WorkspaceService workspaces,
                             BoardRepository boards,
                             CardRepository cards,
                             StringRedisTemplate redis) {
        this.memberships = memberships;
        this.workspaces = workspaces;
        this.boards = boards;
        this.cards = cards;
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

    /** Read access to a workspace = membership in its organization. */
    public boolean canViewWorkspace(String workspaceId, String userId) {
        String orgId = workspaceOrg(workspaceId);
        return orgId != null && isOrgMember(orgId, userId);
    }

    /** Write access to a workspace itself = ADMIN or OWNER. */
    public boolean canWriteWorkspace(String workspaceId, String userId) {
        String orgId = workspaceOrg(workspaceId);
        return orgId != null && isOrgAdmin(orgId, userId);
    }

    /** Write access to a workspace's content (documents, lists, cards, comments) = MEMBER+. */
    public boolean canWriteWorkspaceContent(String workspaceId, String userId) {
        String orgId = workspaceOrg(workspaceId);
        return orgId != null && isOrgWriter(orgId, userId);
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
        String orgId = boardOrg(boardId);
        return orgId != null && isOrgWriter(orgId, userId);
    }

    /** Read a card: any member of the card's board's workspace organization. */
    public boolean canViewCard(String cardId, String userId) {
        String wsId = cardWorkspace(cardId);
        return wsId != null && canViewWorkspace(wsId, userId);
    }

    /** Edit a card or its comments: MEMBER+. */
    public boolean canEditCard(String cardId, String userId) {
        String orgId = cardOrg(cardId);
        return orgId != null && isOrgWriter(orgId, userId);
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
