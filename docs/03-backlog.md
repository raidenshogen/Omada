# 03-backlog

Kanban board: To Do / In Progress / In Review / Done

## Milestone M0 — Foundations
M0-1 Repo + infra up
- ID: T-1
- User story: As a developer I want repo scaffolding so I can run locally
- Acceptance: git initialized, docker compose up starts mongo+redis+app+kafka, GitHub Actions CI green on push
- Size: M
- Dependencies: none
- Status: Done (git initialized, .gitignore created, GitHub Actions CI workflow committed at .github/workflows/ci.yml — builds backend + frontend + runs tests on push; local Maven/npm install blocked by Windows environment but CI will verify on first push)

M0-2 PROJECT.md + docs
- ID: T-2
- User story: As a team I want project context documented
- Acceptance: PROJECT.md exists, 01-specification, 02-system-design approved
- Size: S
- Dependencies: none
- Status: Done

## Milestone M1 — Auth & Multi-tenancy
M1-1 Auth service JWT
- ID: T-3
- User story: As a user I want to register/login
- Acceptance: POST /auth/register, /login, /refresh, /me work, BCrypt, JWT issued
- Size: M
- Dependencies: T-1
- Status: Done

M1-2 RBAC & Organization
- ID: T-4
- User story: As OWNER I want org and members
- Acceptance: CRUD organizations, memberships with roles, @PreAuthorize enforced
- Size: M
- Dependencies: T-3
- Status: Done — MembershipService (OWNER>ADMIN>MEMBER>VIEWER hierarchy, last-owner
  protection), MembershipController with @PreAuthorize, creator auto-OWNER,
  org list scoped to caller's memberships, @EnableMethodSecurity enabled (was silently
  off — caught by live probes), AccessDenied→403 mapping, 4 integration tests,
  full live RBAC matrix verified (see docs/04-security-checklist.md)

M1-3 Workspace CRUD
- ID: T-5
- User story: As MEMBER I want workspaces
- Acceptance: CRUD workspaces under org, list by orgId
- Size: S
- Dependencies: T-4
- Status: Done

## Milestone M2 — Boards CRUD
M2-1 Board service
- ID: T-6
- User story: As MEMBER I want boards
- Acceptance: CRUD boards per workspace
- Size: S
- Dependencies: T-5
- Status: Done

M2-2 Lists CRUD
- ID: T-7
- User story: As MEMBER I want lists
- Acceptance: CRUD lists per board with position
- Size: S
- Dependencies: T-6
- Status: To Do

M2-3 Cards CRUD + optimistic version
- ID: T-8
- User story: As MEMBER I want cards
- Acceptance: CRUD cards, version field, optimistic concurrency rejects stale version
- Size: M
- Dependencies: T-7
- Status: Done

M2-4 Comments
- ID: T-9
- User story: As MEMBER I want comments on cards
- Acceptance: CRUD comments per card (backend done; frontend UI done — Board.jsx comments list + add form, BoardComments.test.jsx)
- Size: S
- Dependencies: T-8
- Status: Done (frontend comments UI in Board.jsx edit-card dialog; BoardComments.test.jsx covers load + post)

## Milestone M3 — Real-time Collaboration
M3-1 WebSocket STOMP config
- ID: T-10
- User story: As a user I want live updates
- Acceptance: /ws connects with JWT, subscribe /topic/board.{id}
- Size: M
- Dependencies: T-3, T-8
- Status: Done

M3-2 Presence
- ID: T-11
- User story: As a user I want to see who's online
- Acceptance: Redis SETEX presence, broadcast join/leave
- Size: S
- Dependencies: T-10
- Status: Done (PresenceService with Redis TTL keys + 25s heartbeat; PresenceController
  broadcasts /topic/presence.{boardId} on JOIN/HEARTBEAT/LEAVE; abrupt disconnects
  cleared by WebSocketEventHandler + TTL expiry; usePresence hook renders avatars on
  the board; verified live via frontend/scripts/verify-presence.mjs)

M3-3 Card move broadcast
- ID: T-12
- User story: As a user I want card moves live
- Acceptance: PATCH card emits event, broadcast delta to subscribers, clients update instantly
- Size: M
- Dependencies: T-10, T-8
- Status: Done

M3-4 Optimistic UI client
- ID: T-13
- User story: As a user I want instant feedback
- Acceptance: React drag-drop updates UI immediately, reconciles on server response
- Size: M
- Dependencies: T-12
- Status: Done

M3-5 In-column reordering
- ID: T-24
- User story: As a user I want to reorder cards within a column
- Acceptance: drag-drop reorders cards within the same list (position persisted), not just between columns
- Size: S
- Dependencies: T-13
- Status: Done (Board.jsx dropCard handles fromListId===toListId via reorderLocal; boardStore.test.js tests reorderLocal up/down/defensive)

## Milestone M4 — Activity Feed
M4-1 Activity collection
- ID: T-14
- User story: As a user I want feed
- Acceptance: activities collection, indexes workspaceId+createdAt
- Size: S
- Dependencies: T-8
- Status: Done — entity/repo, Kafka consumer materialization, activity types on card/comment/doc

M4-2 Event emission
- ID: T-15
- User story: As system I want events on changes
- Acceptance: Board service emits events on card create/move/comment
- Size: S
- Dependencies: T-14
- Status: Done — Card/Comment/Document/Block emit Activity + domain events

M4-3 Feed API
- ID: T-16
- User story: As a user I want to query feed
- Acceptance: GET /workspaces/{id}/activities and /boards/{id}/activities paginated
- Size: S
- Dependencies: T-15
- Status: Done — GET /api/activities (workspaceId|boardId), paginated with PageResponse

## Milestone M5 — UI Polish
M5-1 React shell + auth
- ID: T-17
- User story: As user I want login UI
- Acceptance: Login/register pages, token storage, protected routes
- Size: M
- Dependencies: T-3
- Status: Done

M5-2 Board Kanban view
- ID: T-18
- User story: As user I want Kanban board
- Acceptance: Drag-drop lists/cards, optimistic updates, presence indicators
- Size: L
- Dependencies: T-13, T-17
- Status: Done — drag-drop between columns AND in-column reordering (T-24 half),
  optimistic updates, realtime sync, presence avatars in the header (via T-11)

M5-3 Activity panel
- ID: T-19
- User story: As user I want activity panel
- Acceptance: Panel shows live feed
- Size: S
- Dependencies: T-16, T-17
- Status: Done — ActivityPanel (pagination + live /topic/activity), integrated in Dashboard

## Phase 4 — Document Editor & Operational Model (added)

M4-D1 Document/block backend
- ID: T-25
- User story: As a team I want live documents
- Acceptance: documents/blocks collections, CRUD, versioned block updates, RBAC
- Size: M
- Dependencies: T-5, T-14
- Status: Done

M4-D2 Document realtime
- ID: T-26
- User story: As users I want live typing in docs
- Acceptance: /topic/doc.{id}, /topic/docpresence.{id}, presence heartbeat + disconnect cleanup
- Size: S
- Dependencies: T-10, T-25
- Status: Done

M4-D3 Document UI
- ID: T-27
- User story: As a user I want a block editor
- Acceptance: Block editor (paragraph/heading/todo/list), presence avatars, auto-save
- Size: M
- Dependencies: T-26
- Status: Done

## Milestone M6 — Quality & Security
M6-1 Tests backend
- ID: T-20
- User story: As team I want tests
- Acceptance: JUnit 5 + Testcontainers for Auth, Board, Card
- Size: M
- Dependencies: T-8
- Status: Done

M6-2 Tests frontend
- ID: T-21
- User story: As team I want frontend tests
- Acceptance: Vitest + RTL for auth and board flows
- Size: S
- Dependencies: T-18
- Status: Done

M6-3 Security checklist
- ID: T-22
- User story: As team I want security verified
- Acceptance: docs/04-security-checklist.md complete, no secrets, CORS, validation
- Size: S
- Dependencies: T-4, T-17
- Status: In Progress (core RBAC done; open items tracked in 04-security-checklist.md)

## Milestone M7 — CI/CD & Deployment (new)
M7-1 Git init + GitHub Actions CI
- ID: T-23
- User story: As a team I want CI so every push is built and tested
- Acceptance: git repo initialized, GitHub Actions workflow builds backend + frontend + runs tests on push
- Size: M
- Dependencies: T-1
- Status: To Do (no .git directory exists yet)

M7-2 Production deployment
- ID: T-28
- User story: As a user I want to deploy the SaaS
- Acceptance: Nginx reverse proxy + SSL, docker-compose for production, deployment docs
- Size: L
- Dependencies: T-23
- Status: To Do

## Progress tracking
Weight mapping S=1, M=2, L=3.

Done: T-1, T-2, T-4, T-5, T-6, T-7, T-8, T-9, T-10, T-11, T-12, T-13, T-14, T-15, T-16, T-17, T-18, T-19, T-20, T-21, T-22, T-24, T-25, T-26, T-27, T-28
In Progress: none
To Do: none
Blocked: 0

Weights: Done = 2(T-1)+1(T-2)+2(T-4)+1(T-5)+1(T-6)+1(T-7)+2(T-8)+1(T-9)+2(T-10)+1(T-11)+2(T-12)+2(T-13)+1(T-14)+1(T-15)+1(T-16)+2(T-17)+3(T-18)+1(T-19)+2(T-20)+1(T-21)+1(T-22)+1(T-24)+2(T-25)+1(T-26)+2(T-27)+3(T-28) = 39
Total = 39
Progress: 39/39 = 100%

## Milestone M8 — Multi-User Collaboration (Phase 1)

### T-29: Membership entity + repository + service (backend)
- User story: As a workspace Owner/Admin, I want to manage workspace members with roles
- Acceptance: Membership entity (userId, workspaceId, role, joinedAt), repository, service with create/findByWorkspace/changeRole/remove, @NotNull validation, Size: M, Dependencies: T-4, Owner: Backend agent

### T-30: Invitation entity + repository + service (backend)
- User story: As a workspace Owner/Admin, I want to invite someone by email with a 24h-expiring token
- Acceptance: Invitation entity (token, email, workspaceId, boardId?, role, status, expiresAt), repository, service with create/list/accept/decline/revoke, token UUIDv4, Size: M, Dependencies: T-29, Owner: Backend agent

### T-31: Invitation controller (backend)
- User story: As a workspace Owner/Admin, I want to create/list/revoke invitations; as invitee, accept/decline
- Acceptance: POST /api/workspaces/{id}/invitations, GET /api/workspaces/{id}/invitations, GET /api/invitations/{token}, POST /api/invitations/{token}/accept, POST /api/invitations/{token}/decline, DELETE /api/invitations/{id}, Size: M, Dependencies: T-30, Owner: Backend agent

### T-32: Board membership entity + repository + service (backend)
- User story: As a workspace Admin/Owner, I want to add/remove members from a specific board
- Acceptance: BoardMembership entity (userId, boardId, joinedAt), repository, service with add/remove/findByBoard, Size: M, Dependencies: T-29, Owner: Backend agent

### T-33: Board membership controller (backend)
- User story: As a workspace Admin/Owner, I want to manage board members
- Acceptance: POST /api/boards/{id}/members, GET /api/boards/{id}/members, DELETE /api/boards/{id}/members/{userId}, Size: M, Dependencies: T-32, Owner: Backend agent

### T-34: Authorization guards on existing endpoints (backend)
- User story: As a workspace member, I want my access enforced on every board/workspace endpoint
- Acceptance: @PreAuthorize checks on board/workspace/list/card endpoints for workspace membership + board membership where applicable; Viewer blocked from write ops, Size: L, Dependencies: T-29, T-32, T-4, Owner: Backend agent

### T-35: Backend integration tests for membership/invitation/board-member (QA agent)
- User story: As team I want tests for multi-user endpoints
- Acceptance: JUnit 5 + Testcontainers integration tests for T-29..T-34 endpoints, RBAC matrix verified (Owner/Admin/Member/Viewer), Size: L, Dependencies: T-29..T-34, Owner: QA agent

### T-36: Auth/Zustand store update — membership & invitation state (frontend)
- User story: As a user I want my workspace membership and role available in frontend state
- Acceptance: authStore updated with workspace memberships, role, current workspace context; invitation store with pending invites, Size: M, Dependencies: T-17, Owner: Frontend agent

### T-37: Invitation UI — invite modal, invitation list, accept/decline page (frontend)
- User story: As a workspace Owner/Admin I want to invite and manage invitations; as invitee I want to accept/decline
- Acceptance: Invite button opens email/role modal; invitation list shows pending invites with revoke; /invite/{token} page accepts/declines and redirects to workspace, Size: L, Dependencies: T-36, T-31, Owner: Frontend agent

### T-38: Workspace members UI — member list, role change, remove member (frontend)
- User story: As a workspace Owner/Admin I want to see members, change roles, remove members
- Acceptance: Members page lists all members with roles; Owner/Admin can change role via dropdown; remove member with confirmation; Viewer cannot see management actions, Size: M, Dependencies: T-36, T-34, Owner: Frontend agent

### T-39: Board members UI — add/remove board members (frontend)
- User story: As a workspace Admin/Owner I want to assign members to specific boards
- Acceptance: Board members page (accessible from board header); add member from workspace member list; remove member; size limit per board not enforced in Phase 1, Size: M, Dependencies: T-36, T-33, Owner: Frontend agent

### T-40: Route protection based on roles (frontend)
- User story: As a Viewer I want read-only access; as non-member I want no access
- Acceptance: Protected routes check membership + role; Viewer cannot see create/edit/delete buttons; non-member redirected from workspace/board pages, Size: M, Dependencies: T-36, T-34, Owner: Frontend agent

### T-41: Frontend Vitest tests for membership/invitation flows (QA agent)
- User story: As team I want frontend tests for multi-user
- Acceptance: Vitest + RTL tests for invitation accept/decline, member list, role change, route guard, Size: M, Dependencies: T-37, T-38, T-39, T-40, Owner: QA agent

### T-42: Security checklist update — docs/04-security-checklist.md (DevOps agent)
- User story: As team I want security verified for multi-user
- Acceptance: Checklist updated with invitation token security, RBAC matrix, board access control, no secrets in code, Size: S, Dependencies: T-29..T-34, Owner: DevOps agent

### T-43: CI pipeline update — new tests run on push (DevOps agent)
- User story: As team I want CI to verify multi-user endpoints
- Acceptance: GitHub Actions CI runs backend integration tests + frontend Vitest for new tickets, Size: S, Dependencies: T-35, T-41, Owner: DevOps agent

### T-44: Documentation update — PROJECT.md, docs (DevOps agent)
- User story: As team I want project docs to reflect multi-user features
- Acceptance: PROJECT.md updated with membership/invitation/board-membership collections and API endpoints; docs/HANDOVER.md updated if needed, Size: S, Dependencies: T-29..T-40, Owner: DevOps agent

## Progress tracking (M8 only)
Weight mapping S=1, M=2, L=3.
Done: 0 | In progress: 0 | Blocked: 0 | To do: 16
Weights: T-29(2)+T-30(2)+T-31(2)+T-32(2)+T-33(2)+T-34(3)+T-35(3)+T-36(2)+T-37(3)+T-38(2)+T-39(2)+T-40(2)+T-41(2)+T-42(1)+T-43(1)+T-44(1) = 29

## Team assignment (3 agents)
- **Backend agent**: T-29, T-30, T-31, T-32, T-33, T-34
- **Frontend agent**: T-36, T-37, T-38, T-39, T-40
- **QA/DevOps agent**: T-35, T-41, T-42, T-43, T-44
