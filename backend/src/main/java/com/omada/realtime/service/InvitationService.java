package com.omada.realtime.service;

import com.omada.realtime.entity.Invitation;

import java.util.List;
import java.util.UUID;

/**
 * Workspace invitation management. Roles: OWNER > ADMIN > MEMBER > VIEWER.
 * Statuses: PENDING, ACCEPTED, DECLINED, EXPIRED, REVOKED.
 */
public interface InvitationService {

    /** Creates a PENDING invitation with a UUIDv4 token and expiresAt 24h from now. */
    Invitation create(String workspaceId, String email, String role, String boardId);

    /** Lists all invitations of a workspace. */
    List<Invitation> listByWorkspace(String workspaceId);

    /** Returns the invitation identified by token, throwing if unknown. */
    Invitation getByToken(UUID token);

    /**
     * Accepts the invitation identified by token.
     * Idempotent: accepting an already-accepted invitation does not create a second membership.
     */
    Invitation accept(UUID token);

    /** Declines the invitation identified by token. */
    Invitation decline(UUID token);

    /** Marks the invitation identified by token as revoked. */
    void revoke(UUID token);
}
