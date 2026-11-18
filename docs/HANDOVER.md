# HANDOVER

Welcome to Omada. This doc lets a new developer be productive in ~1 hour.

## Folder map
Omada/
  backend/ Spring Boot monolith
    src/main/java/com/omada/realtime/
      controller/ REST controllers
      service/ interfaces + impl
      repository/ Mongo repositories
      entity/ Mongo documents with UUID ids
      dto/ request/response
      mapper/ MapStruct mappers
      security/ JwtUtil, JwtAuthenticationFilter, IdGenerator
      config/ WebSocket, Mongo, Redis, Kafka, Security
  frontend/ React Vite SPA
    src/pages/ Login, Register, Dashboard, Workspace, Board
    src/components/ Sidebar, CardItem, ActivityFeed
    src/store/ Zustand stores
    src/services/ API client, WebSocket service
  docs/ 01-specification, 01-specification-multiuser, 02-system-design, 03-backlog, 04-security-checklist
  backend/infrastructure/docker/docker-compose.yml
Multi-user data: MongoDB collections workspace_memberships, invitations, board_memberships
(org-scoped memberships live in memberships). Multi-user controllers:
MembershipController (org members), InvitationController (workspace invitations),
BoardMembershipController (board members).

## How to run/test/debug
Run infra:
cd backend/infrastructure/docker && docker compose up -d
Run backend:
cd backend && mvn spring-boot:run   (use mvn.cmd on this Windows host; no mvnw in repo)
Run frontend:
cd frontend && npm install && npm run dev

Tests:
Backend: mvn test  (requires Docker; Testcontainers starts Mongo/Redis/Kafka)
  Multi-user coverage: WorkspaceMembershipServiceIntegrationTest,
  InvitationServiceIntegrationTest, MembershipServiceIntegrationTest (green);
  InvitationControllerSecurityTest fails to load its ApplicationContext
  (blocked, see Known issues)
Frontend: npm test  (vitest run)
  Multi-user coverage: src/__tests__/authStore.test.js, boardStore.test.js and
  page/component tests (Login, Register, Board, Workspace, ActivityPanel, ...)

Debug: Spring Boot devtools enabled, React Vite HMR.

## Key decisions
Monolith for MVP with service layer separation
MongoDB UUID ids for flexible schema
Optimistic locking via version field on Card
WebSocket STOMP for live updates
JWT stateless auth with refresh tokens
Multi-user: workspace memberships (OWNER > ADMIN > MEMBER > VIEWER), email invitations
(UUIDv4 token, 24h expiry), board memberships; RBAC via @PreAuthorize + WorkspaceSecurity;
org-scoped Membership (T-4) kept separate from workspace-scoped WorkspaceMembership
SPA with optimistic UI for Trello/Notion feel

## Conventions
Constructor injection only, final fields
Controller -> Service -> Repository
DTOs for all API boundaries
MapStruct for mapping
Javadoc on public classes/methods
No secrets in code, env vars only

## Known issues
Maven wrapper not on PATH, use local mvn (apache-maven 3.9.15; use mvn.cmd on this host)
WebSocket JWT auth interceptor not fully wired
Activity feed event emission not connected to Kafka yet
No dedicated WorkspaceMembership controller: workspace membership CRUD
(create/findByWorkspace/changeRole/remove) exists at the service layer only;
org-scoped members are exposed via /api/organizations/{orgId}/members
Invitation get/accept/decline by token are permitAll — the unguessable UUIDv4 token
is the grant, so treat invitation links as sensitive (24h expiry limits exposure)
Presence heartbeats not implemented (Redis is used only for workspace-org RBAC
caching in WorkspaceSecurity)
scripts/verify-realtime.mjs removed from frontend/scripts
Backend `mvn test` blocked at test-compile: BoardMembershipControllerIntegrationTest
and InvitationControllerIntegrationTest import spring-security-test, which is not
resolvable on the build classpath (.m2 artifact needs a redownload)

## Next tickets
Presence Redis (heartbeats; Redis currently only caches workspace-org for RBAC)
In-column reordering, comments UI, document editor
Production deployment (T-28): Nginx + SSL, production docker-compose
CI workflow (T-23): GitHub Actions build + backend + frontend tests on push
