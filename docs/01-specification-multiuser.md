# Omada Multi-User Collaboration Specification

## Problem and Goals

Omada is currently single-user: boards, lists, and cards exist, but there is no concept of teams, workspace members, or invitations. Only one person can use a workspace.

**Goal:** Add multi-user collaboration so a workspace owner can invite teammates by email, assign them roles, and control which boards they can access — similar to Trello/Notion.

## Users and Roles (Workspace Level)

| Role | Can Create/Edit/Delete | Can Invite Members | Can Change Roles | Can Delete Workspace |
|------|----------------------|-------------------|-----------------|---------------------|
| Owner | Yes | Yes | Yes | Yes |
| Admin | Yes | Yes | Yes | No |
| Member | Yes (on assigned boards) | No | No | No |
| Viewer | No (read-only) | No | No | No |

## Scope

### In Scope
- Invite users to a workspace by email with a unique token link.
- Invitations expire after 24 hours; can be declined or revoked by the inviter.
- On acceptance, user becomes a workspace member and is redirected to the workspace.
- Workspace membership with roles (Owner, Admin, Member, Viewer).
- Board-level membership: a user can belong to a workspace but not to a specific board; Admin/Owner assigns users to boards.
- Maximum 20 members per workspace.
- Email notification when a user is assigned to a card (task). No broadcast emails.
- API endpoints for listing, inviting, accepting, declining, revoking, and removing members at workspace and board level.

### Out of Scope (Phase 1)
- Real SMTP email delivery (invitations stored and accept link shown/logged; SMTP can be wired later).
- Board-level roles (board membership inherits the workspace role).
- Card-level permissions beyond board membership.
- Bulk user import.

## Functional Requirements (User Stories)

### Invitations
- As a workspace Owner/Admin, I want to invite someone to my workspace by email so they can join and collaborate.
- As a workspace Owner/Admin, I want to see all pending invitations for my workspace so I can track or revoke them.
- As a workspace Owner/Admin, I want to revoke a pending invitation before it is accepted.
- As an invitee, I want to receive a unique link (valid 24 hours) so I can accept or decline.
- As an invitee, after accepting, I want to be redirected to the workspace.
- As an invitee, if I do not respond within 24 hours, my invitation expires and I must request a new one.

### Workspace Membership
- As a workspace member, I want to see who else is in my workspace and their roles.
- As a workspace Owner/Admin, I want to change a member's role (e.g., Member → Admin).
- As a workspace Owner/Admin, I want to remove a member from the workspace so they lose access to all its boards.

### Board Membership
- As a workspace Admin/Owner, I want to add a workspace member to a specific board.
- As a workspace Admin/Owner, I want to remove someone from a specific board while keeping them in the workspace.
- As a board member, I can see and interact with the board according to my workspace role.
- As a non-board member, I cannot see or access the board even if I am in the workspace.

### Notifications
- As a user, I want to receive an email when someone assigns me to a card.
- As a user, I do not want email notifications for every card change — only when I am directly assigned.

### Authorization
- As a Viewer, I cannot create, edit, or delete boards, lists, or cards.
- As a non-member, I cannot access a workspace or board at all.

## Non-Functional Requirements
- **Security:** Invitations use unguessable tokens; authorization enforced on every endpoint.
- **Performance:** Member lists and board access checks use indexed queries.
- **Reliability:** Invitation acceptance is idempotent.
- **Maintainability:** Membership logic isolated in dedicated services; controllers remain thin.

## Assumptions
- Users already exist (auth system: register/login/JWT).
- Email delivery is simulated in Phase 1 (invitation link shown in UI and logged); real SMTP can be added later.
- Board membership inherits the workspace role (no separate board roles in Phase 1).
- A user has at most one active membership per workspace and one per board.

## Risks
- **Invitation token leakage:** Mitigation — cryptographically random tokens (UUIDv4).
- **Role escalation:** Mitigation — role-change endpoint restricted to Owner/Admin and validated server-side.
- **Stale invitations:** Mitigation — check `expiresAt` before accepting.

## Acceptance Criteria
1. Owner/Admin can create an email invitation for a workspace; it returns a token/link.
2. Invitation link accepts/declines; acceptance creates a workspace membership with the assigned role.
3. Invitation expires after 24 hours and cannot be accepted afterward.
4. Workspace member list shows all members with roles; Owner/Admin can change roles or remove members.
5. Board member list shows who is on each board; Owner/Admin can add/remove board members.
6. A workspace member not on a board cannot see or open that board.
7. Viewer role cannot create/edit/delete anything.
8. Maximum 20 members enforced per workspace.
9. All new endpoints have integration tests; existing tests still pass.
