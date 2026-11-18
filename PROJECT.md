# Omada — Real-Time Collaboration SaaS

## Purpose
Productivity tool with live collaboration like Trello / Notion. Live editing of boards & documents, team workspaces, role-based access, activity feed. Portfolio demo, zero production infra cost.

## Stack
Backend: Spring Boot 3.3.2, Java 17, Spring Web / WebSocket STOMP, Spring Security JWT, Spring Data MongoDB, Spring Kafka, MapStruct, Lombok, JJWT
Frontend: React 18, Vite 5, React Router 6, Zustand, @stomp/stompjs, SockJS, Framer Motion, TailwindCSS, Axios
Data: MongoDB 7, Redis 7
Infra: Docker Compose, Nginx, GitHub Actions CI

## Repo layout
Omada/
  ARCHITECTURE.md
  MVP_DESIGN.md
  ROADMAP.md
  PROJECT.md
  backend/
    pom.xml
    src/main/java/com/omada/realtime/...
    src/main/resources/application.yml
    infrastructure/docker/docker-compose.yml
  frontend/
    package.json
    src/

## Key decisions
- Monolith Spring Boot for MVP with service-layer separation for Auth, Workspace, Board, Card, Comment, Activity. Services map to MVP_DESIGN.md bounded contexts.
- MongoDB for flexible board/document schema, version field on cards for optimistic concurrency. Reference fields (workspaceId, boardId, listId) are stored and queried as Strings — UUID-typed queries against String fields silently match nothing (BSON type mismatch); this bug class is covered by integration tests.
- JWT access 15m + refresh 7d, stateless. JwtAuthenticationFilter installs the user id as principal; RBAC rules via @PreAuthorize enforced through WorkspaceSecurity (OWNER > ADMIN > MEMBER > VIEWER hierarchy resolved from membership records).
- Multi-user: workspace memberships (memberships collection) with roles Owner/Admin/Member/Viewer; email invitations (invitations collection, token UUIDv4, 24h expiry); board memberships (board_memberships collection). RBAC enforced via @PreAuthorize on all workspace/board/list/card endpoints.
- Realtime: card mutations publish a Spring ApplicationEvent (Observer) → BoardSocketPublisher broadcasts to STOMP topic /topic/board.{boardId}; frontend boardStore.applyRemoteEvent applies remote events (idempotent, handles own echo). SockJS transport on /ws, JWT required at CONNECT.
- Kafka for activity event bus (topic `activities`, consumer group omada-activity-feed) → ActivityConsumer materializes the feed into MongoDB. CLUSTER_ID is set in both compose files (KRaft).
- Frontend React + Vite, Zustand stores, STOMP client via sockjs-client (the backend endpoint is SockJS-only, so a plain ws:// brokerURL does not work).
- Local-first Docker Compose: kafka (KRaft), mongo, redis, app at repo root. No production deployment.

## Data model highlights
users, organizations, workspaces, boards, lists (TaskList), cards, comments, activities, memberships, invitations, board_memberships.
IDs are UUIDs generated in the service layer via IdGenerator.uuid(); reference fields are Strings.
Collections in MongoDB, indexes per MVP_DESIGN.md.

## API
Base /api
Auth: POST /auth/register, POST /auth/login, POST /auth/refresh, GET /auth/me
Organizations, Workspaces, Boards, Lists, Cards (PATCH accepts listId+position for drag & drop), Comments, Activity Feed
Multi-user: org memberships POST/GET /api/organizations/{orgId}/members,
PATCH/DELETE /api/organizations/{orgId}/members/{userId} (role change on PATCH);
workspace invitations POST/GET /api/workspaces/{id}/invitations,
GET /api/invitations/{token}, POST /api/invitations/{token}/accept|decline,
DELETE /api/invitations/{id} (id = invitation token); board memberships
POST/GET /api/boards/{id}/members, DELETE /api/boards/{id}/members/{userId}.
Workspace-membership CRUD (create/findByWorkspace/changeRole/remove) is
service-layer only — no dedicated controller.
WebSocket: STOMP over SockJS /ws (Authorization: Bearer *** at CONNECT), subscribe /topic/board.{boardId};
payloads {type: CARD_CREATED|CARD_UPDATED|CARD_DELETED|MEMBER_ADDED|MEMBER_REMOVED|ROLE_CHANGED, boardId, listId, previousListId?, card, userId, role}

## Non-functional
Security: JWT Bearer, BCrypt, CORS locked, RBAC per service, no secrets in repo.
Performance: Stateless services, horizontal scale, Mongo indexes, Redis presence.
Quality: JUnit 5, Testcontainers, Vitest + RTL, SonarQube quality gate, dependency check.

## Constraints
- No production infra cost. Local dev only, Docker Compose.
- MVP scope: users, workspaces, boards with lists/cards, comments, activity feed. No full document editor yet.
- CI build+test only, Ansible playbooks documented but not executed.

## Current state
Working end-to-end (verified on the Docker stack): register/login (real JWT+BCrypt), orgs,
workspaces, boards with seeded Todo/Doing/Done lists, card create/edit/delete, drag & drop
between columns (persisted + optimistic), realtime WebSocket sync of card changes, activity
feed via Kafka consumer. Frontend pages (Dashboard/Workspace/Board/Sidebar) are fully wired
to the API; sidebar is workspace/board navigation, hidden for guests.
Tests: backend service-layer integration tests green (Testcontainers Mongo/Redis/Kafka,
incl. Kafka activity flow and Phase-1 multi-user service tests for workspace memberships,
invitations, board memberships). Full `mvn test` is blocked at test-compile: two new
controller integration tests import spring-security-test, which is not resolvable on the
build classpath (corrupted .m2 artifact — needs a redownload); controller slice tests
error on context load (BoardMembershipRepository bean missing in WebMvcTest slices after
the WorkspaceSecurity RBAC change). Frontend: 24 Vitest tests green (incl. realtime store
logic and auth/board store coverage).
Global exception handler returns {message} JSON errors.
Phase 1 Multi-User (done, T-29..T-40, docs/03-backlog.md milestone M8): workspace
memberships (OWNER/ADMIN/MEMBER/VIEWER), email invitations with UUIDv4 tokens (24h
expiry), board memberships, RBAC via @PreAuthorize on workspace/board/list/card
endpoints (WorkspaceSecurity), invitation accept/decline UI, member list/role-change/
remove UI, role-based route protection.
Not done: production deployment.
  (Local-first Docker Compose only — no production infra cost, by design for MVP portfolio demo.)

## Ownership
Project owner: Zena/Zeo. Lead engineer Miles per SOUL.md lifecycle.
